import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "GET, POST, PATCH, PUT, DELETE, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};
const reply = (data: unknown, status = 200) => new Response(JSON.stringify(data), { status, headers: corsHeaders });
const api = async (path: string, token?: string, init: RequestInit = {}) => {
  const headers = new Headers(init.headers || {});
  headers.set("apikey", ANON_KEY);
  headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  else headers.set("Authorization", `Bearer ${ANON_KEY}`);
  const res = await fetch(`${SUPABASE_URL}/rest/v1/${path}`, { ...init, headers });
  const body = await res.text();
  if (!res.ok) throw new Error(body || `Supabase REST HTTP ${res.status}`);
  return body ? JSON.parse(body) : null;
};
const authUser = async (token: string) => {
  const r = await fetch(`${SUPABASE_URL}/auth/v1/user`, { headers: { apikey: ANON_KEY, Authorization: `Bearer ${token}` } });
  if (!r.ok) return null;
  return await r.json();
};
const tokenOf = (req: Request) => (req.headers.get("authorization") || "").replace(/^Bearer\s+/i, "");
const safeObjectPath = (value: string) => value && !value.startsWith("/") && !value.includes("..") && !value.includes("\\") && value.split("/").every(part => /^[a-zA-Z0-9._-]+$/.test(part));
const getStoredObjectInfo = async (objectPath: string, token: string) => {
  if (!safeObjectPath(objectPath)) return null;
  const encodedPath = objectPath.split("/").map(encodeURIComponent).join("/");
  const response = await fetch(`${SUPABASE_URL}/storage/v1/object/info/post-media/${encodedPath}`, {
    headers: { apikey: ANON_KEY, Authorization: `Bearer ${token}` }
  });
  if (!response.ok) return null;
  return await response.json().catch(() => null);
};
const objectMimeType = (info: any) => String(info?.metadata?.mimetype || info?.metadata?.contentType || info?.contentType || "").toLowerCase();
const profileById = async (id: string, token: string, preferredUsername = "") => {
  const rows = await api(`profiles?id=eq.${encodeURIComponent(id)}&select=*`, token);
  if (!rows?.length) {
    const generated = `user_${id.replaceAll("-", "").slice(0,12)}`;
    const username = preferredUsername.trim().toLowerCase().replace(/[^a-z0-9_.]/g, "").slice(0,30) || generated;
    await api("profiles", token, { method: "POST", headers: { Prefer: "resolution=ignore-duplicates,return=minimal" }, body: JSON.stringify([{ id, username, display_name: preferredUsername.trim(), bio: "" }]) });
    const fresh = await api(`profiles?id=eq.${encodeURIComponent(id)}&select=*`, token);
    return fresh?.[0] || { id, username, display_name: preferredUsername.trim(), bio: "" };
  }
  return rows[0];
};
const profileDto = (p: any) => ({ id:p.id, username:p.username, email:p.email || "", displayName:p.display_name || "", bio:p.bio || "", avatarUrl:p.avatar_path ? (p.avatar_path.startsWith("http") ? p.avatar_path : `${SUPABASE_URL}/storage/v1/object/public/post-media/${p.avatar_path}`) : "", coverUrl:p.cover_path ? (p.cover_path.startsWith("http") ? p.cover_path : `${SUPABASE_URL}/storage/v1/object/public/post-media/${p.cover_path}`) : null, verified:!!p.is_verified, createdAt:p.created_at });
Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  try {
    const url = new URL(req.url);
    // Supabase gateway versions may expose the function slug in the pathname differently.
    // Normalize to the API route so /api/auth/google cannot fall through into the authenticated profile guard.
    const apiRouteIndex = url.pathname.indexOf("/api/");
    const normalizedPath = apiRouteIndex >= 0 ? url.pathname.slice(apiRouteIndex) : url.pathname.replace(/^\/functions\/v1\/vybora-api/, "");
    const path = normalizedPath.replace(/\/$/, "") || "/";
    const token = tokenOf(req);
    const body = ["POST","PATCH","PUT","DELETE"].includes(req.method) ? await req.json().catch(() => ({})) : {};
    // Accept both the legacy custom bearer and a valid Supabase session only after checking Auth.
    // No anonymous or fabricated token is ever treated as an authenticated user.
    if (req.method === "POST" && path === "/api/auth/google") {
      const idToken = String(body.idToken || body.id_token || "");
      if (!idToken) return reply({error:"Credencial Google ausente."},400);
      const r = await fetch(`${SUPABASE_URL}/auth/v1/token?grant_type=id_token`, { method:"POST", headers:{apikey:ANON_KEY,"Content-Type":"application/json"}, body:JSON.stringify({provider:"google", id_token:idToken}) });
      const data = await r.json();
      if (!r.ok) {
        console.error("Google Auth rejected token:", JSON.stringify({status:r.status,error:data.error,code:data.code,message:data.msg || data.message || data.error_description || data.error_description}));
        const detail = data.error_description || data.msg || data.message || data.error || data.code;
        // Surface Supabase's actual rejection reason to the app; do not replace it with a generic OAuth hint.
        const diagnostic = [detail, data.code && data.code !== detail ? `Código: ${data.code}` : ""].filter(Boolean).join(" — ");
        return reply({
          error: diagnostic || `O Supabase recusou o token do Google (HTTP ${r.status}).`,
          code: data.code || data.error || "google_auth_failed"
        }, r.status);
      }
      const p = await profileById(data.user.id, data.access_token, String(data.user.user_metadata?.username || data.user.user_metadata?.full_name || data.user.email?.split("@")[0] || ""));
      return reply({token:data.access_token, refreshToken:data.refresh_token, user:{...profileDto({...p, email:data.user.email}), username:p.username || `user_${data.user.id.replaceAll("-","").slice(0,12)}`}});
    }
    // Refresh a Supabase session using its refresh token; never accept an access token as a refresh credential.
    if (req.method === "POST" && path === "/api/auth/refresh") {
      const refreshToken = String(body.refreshToken || body.refresh_token || "").trim();
      if (!refreshToken || refreshToken.length > 4096) return reply({error:"Sessão vencida. Entre novamente na sua conta."},401);
      const refreshed = await fetch(`${SUPABASE_URL}/auth/v1/token?grant_type=refresh_token`, {
        method:"POST",
        headers:{apikey:ANON_KEY,"Content-Type":"application/json"},
        body:JSON.stringify({refresh_token:refreshToken})
      });
      const data = await refreshed.json().catch(() => ({}));
      if (!refreshed.ok || !data.access_token || !data.refresh_token || !data.user?.id) {
        return reply({error:"Não foi possível renovar a sessão. Entre novamente na sua conta.", code:data.code || data.error || "refresh_failed"},401);
      }
      const p = await profileById(data.user.id,data.access_token,String(data.user.user_metadata?.username || data.user.email?.split("@")[0] || ""));
      return reply({token:data.access_token,refreshToken:data.refresh_token,user:profileDto({...p,email:data.user.email})});
    }
    if (req.method === "POST" && (path === "/api/auth/login" || path === "/api/auth/register")) {
      const register = path.endsWith("register");
      const r = await fetch(`${SUPABASE_URL}/auth/v1/${register ? "signup" : "token?grant_type=password"}`, {method:"POST",headers:{apikey:ANON_KEY,"Content-Type":"application/json"},body:JSON.stringify({email:String(body.email||"").trim().toLowerCase(),password:String(body.password||""), ...(register?{data:{username:String(body.username||"").trim().toLowerCase()}}:{})})});
      const data = await r.json();
      if (!r.ok) return reply({error:data.msg || data.message || data.error_description || "Autenticação recusada."},r.status);
      if (!data.access_token) return reply({error:"Confirme o e-mail para concluir o cadastro."},202);
      const p = await profileById(data.user.id,data.access_token,String(data.user.user_metadata?.username || body.username || data.user.email?.split("@")[0] || ""));
      return reply({token:data.access_token,refreshToken:data.refresh_token,user:profileDto({...p,email:data.user.email})},register?201:200);
    }
    // Public read routes are allowed before authentication; database RLS still applies.
    if (req.method==="GET" && path==="/api/posts") {
      const posts=await api("posts?select=*,profiles(id,username,display_name,avatar_path)&visibility=eq.public&order=created_at.desc&limit=30");
      return reply({posts:await Promise.all((posts||[]).map(async (p:any)=>({id:p.id,caption:p.caption,mediaUrl:p.media_path?.startsWith("http")?p.media_path:(p.media_path?`${SUPABASE_URL}/storage/v1/object/public/post-media/${p.media_path}`:""),mediaType:p.media_type,author:{id:p.profiles?.id,username:p.profiles?.username,displayName:p.profiles?.display_name},likes:0,comments:0})))});
    }
    if (req.method==="GET" && path.startsWith("/api/posts/") && path.endsWith("/comments")) {
      const postId=path.split("/")[3];
      const rows=await api(`comments?post_id=eq.${postId}&select=*,profiles(username,display_name)&order=created_at.asc`);
      return reply({comments:(rows||[]).map((c:any)=>({id:c.id,body:c.body,author:{username:c.profiles?.username,displayName:c.profiles?.display_name}}))});
    }
    if (req.method==="GET" && path.startsWith("/api/users/") && !path.includes("/me") && !path.endsWith("/followers") && !path.endsWith("/following")) {
      const name=decodeURIComponent(path.split("/")[2]||"").replace(/^@/,"").toLowerCase();
      const rows=await api(`profiles?username=eq.${encodeURIComponent(name)}&select=*`);
      return rows?.[0]?reply({user:profileDto(rows[0])}):reply({error:"Perfil não encontrado."},404);
    }
    if (req.method==="GET" && path==="/api/users/search") {
      const q=url.searchParams.get("q")||"";
      const rows=await api(`profiles?or=(username.ilike.*${encodeURIComponent(q)}*,display_name.ilike.*${encodeURIComponent(q)}*)&select=id,username,display_name,bio,avatar_path,is_verified&limit=30`);
      return reply({users:(rows||[]).map((p:any)=>profileDto(p))});
    }
    if (!token) return reply({error:"Sua sessão expirou ou não é mais válida. Entre novamente com o Google para continuar."},401);
    const user = await authUser(token);
    if (!user?.id) return reply({error:"Sua sessão expirou ou não é mais válida. Entre novamente com o Google para continuar."},401);
    const me = () => profileById(user.id,token);
    if (req.method === "GET" && path === "/api/auth/me") return reply({user:profileDto({...await me(),email:user.email})});
    if (req.method === "PATCH" && path === "/api/users/me") {
      const payload:any = {};
      if (body.username) payload.username=String(body.username).trim().toLowerCase();
      if (body.displayName !== undefined) payload.display_name=String(body.displayName).trim();
      if (body.bio !== undefined) payload.bio=String(body.bio).trim();
      payload.updated_at=new Date().toISOString();
      const rows=await api(`profiles?id=eq.${user.id}&select=*`,token,{method:"PATCH",headers:{Prefer:"return=representation"},body:JSON.stringify(payload)});
      return reply({user:profileDto({...rows?.[0],email:user.email})});
    }
    if (req.method === "POST" && path === "/api/files/upload-url") {
      const category = String(body.category || "");
      const contentType = String(body.contentType || "").toLowerCase();
      const sizeBytes = Number(body.sizeBytes);
      if (!["avatar", "cover", "post_media"].includes(category)) return reply({ error: "Categoria de arquivo inválida." }, 400);
      const imageTypes = new Set(["image/jpeg", "image/png", "image/webp", "image/gif"]);
      const videoTypes = new Set(["video/mp4", "video/quicktime", "video/webm"]);
      const allowed = category === "post_media" ? (imageTypes.has(contentType) || videoTypes.has(contentType)) : imageTypes.has(contentType);
      const maxBytes = category === "post_media" && contentType.startsWith("video/") ? 100 * 1024 * 1024 : 25 * 1024 * 1024;
      if (!allowed) return reply({ error: "Tipo de arquivo não permitido para esta categoria." }, 415);
      if (!Number.isSafeInteger(sizeBytes) || sizeBytes < 1 || sizeBytes > maxBytes) return reply({ error: `Arquivo inválido ou maior que ${Math.floor(maxBytes / 1024 / 1024)} MB.` }, 413);
      const name = String(body.fileName || "file").replace(/[^a-zA-Z0-9._-]/g, "_").slice(-100);
      const objectPath = `${user.id}/${category}/${crypto.randomUUID()}-${name}`;
      return reply({ file: { id: objectPath, category, contentType, sizeBytes }, uploadUrl: `${SUPABASE_URL}/storage/v1/object/post-media/${objectPath}`, objectPath, method: "PUT", requiredHeaders: { "Content-Type": contentType }, expiresInSeconds: null }, 201);
    }
        const fileComplete = path.match(/^\/api\/files\/(.+)\/complete$/);
    if (req.method === "POST" && fileComplete) {
      const objectPath = decodeURIComponent(fileComplete[1]);
      if (!safeObjectPath(objectPath) || !objectPath.startsWith(`${user.id}/`)) return reply({ error: "Arquivo inválido para esta conta." }, 403);
      const parts = objectPath.split("/");
      if (parts.length < 3 || !["avatar", "cover", "post_media"].includes(parts[1])) return reply({ error: "Caminho de mídia inválido." }, 400);
      const info = await getStoredObjectInfo(objectPath, token);
      if (!info) return reply({ error: "O arquivo ainda não foi enviado à nuvem ou não pode ser acessado. Tente enviar novamente." }, 422);
      const mime = objectMimeType(info);
      if ((parts[1] === "avatar" || parts[1] === "cover") && !mime.startsWith("image/")) return reply({ error: "Foto de perfil/capa precisa ser uma imagem." }, 415);
      if (parts[1] === "post_media" && !(mime.startsWith("image/") || mime.startsWith("video/"))) return reply({ error: "A publicação precisa conter uma imagem ou vídeo." }, 415);
      return reply({ ok: true, fileId: objectPath, status: "ready" });
    }
        const profileMedia = path.match(/^\/api\/users\/me\/(avatar|cover)$/);
    if (req.method === "PATCH" && profileMedia) {
      const category = profileMedia[1];
      const col = category === "cover" ? "cover_path" : "avatar_path";
      const objectPath = String(body.fileId || "");
      if (!safeObjectPath(objectPath) || !objectPath.startsWith(`${user.id}/${category}/`)) return reply({ error: "Arquivo inválido para esta conta." }, 403);
      const info = await getStoredObjectInfo(objectPath, token);
      if (!info || !objectMimeType(info).startsWith("image/")) return reply({ error: "A imagem não foi encontrada ou não passou na validação. Envie novamente." }, 422);
      const rows = await api(`profiles?id=eq.${user.id}&select=*`, token, { method: "PATCH", headers: { Prefer: "return=representation" }, body: JSON.stringify({ [col]: objectPath, updated_at: new Date().toISOString() }) });
      return reply({ user: profileDto({ ...rows?.[0], email: user.email }) });
    }
        if (req.method==="GET" && path==="/api/posts") {
      const posts=await api("posts?select=*,profiles(id,username,display_name,avatar_path)&visibility=eq.public&order=created_at.desc&limit=30",token);
      return reply({posts:await Promise.all((posts||[]).map(async (p:any)=>({id:p.id,caption:p.caption,mediaUrl:p.media_path?.startsWith("http")?p.media_path:(p.media_path?`${SUPABASE_URL}/storage/v1/object/public/post-media/${p.media_path}`:""),mediaType:p.media_type,author:{id:p.profiles?.id,username:p.profiles?.username,displayName:p.profiles?.display_name},likes:0,comments:0})))});
    }
    if (req.method==="POST" && path==="/api/posts") {
      const caption = String(body.caption || "").trim();
      const mediaType = String(body.mediaType || "text");
      const mediaFileId = String(body.mediaFileId || "");
      if (caption.length > 5000) return reply({ error: "A legenda deve ter no máximo 5000 caracteres." }, 400);
      if (!["text", "image", "video"].includes(mediaType)) return reply({ error: "Tipo de publicação inválido." }, 400);
      let mediaPath: string | null = null;
      if (mediaType === "text") {
        if (mediaFileId) return reply({ error: "Publicação de texto não deve conter arquivo." }, 400);
      } else {
        if (!safeObjectPath(mediaFileId) || !mediaFileId.startsWith(`${user.id}/post_media/`)) return reply({ error: "A mídia precisa pertencer à sua conta." }, 403);
        const info = await getStoredObjectInfo(mediaFileId, token);
        const mime = objectMimeType(info);
        if (!info || !(mediaType === "image" ? mime.startsWith("image/") : mime.startsWith("video/"))) return reply({ error: "A mídia não foi enviada corretamente à nuvem. Envie novamente antes de publicar." }, 422);
        mediaPath = mediaFileId;
      }
      const row = { user_id: user.id, caption, media_type: mediaType, media_path: mediaPath, visibility: "public" };
      const rows = await api("posts?select=*", token, { method: "POST", headers: { Prefer: "return=representation" }, body: JSON.stringify([row]) });
      return reply({ post: rows?.[0] }, 201);
    }
    const postMatch=path.match(/^\/api\/posts\/([0-9a-f-]+)(?:\/(like|comments))?$/i);
    if(postMatch){
      const id=postMatch[1], action=postMatch[2];
      if(req.method==="DELETE"&&!action){await api(`posts?id=eq.${id}&user_id=eq.${user.id}`,token,{method:"DELETE"});return reply({ok:true});}
      if(action==="like"&&req.method==="POST"){
        const existing=await api(`post_likes?post_id=eq.${id}&user_id=eq.${user.id}&select=user_id`,token);
        if(existing?.length){await api(`post_likes?post_id=eq.${id}&user_id=eq.${user.id}`,token,{method:"DELETE"});return reply({liked:false});}
        await api("post_likes",token,{method:"POST",body:JSON.stringify([{post_id:id,user_id:user.id}])});return reply({liked:true});
      }
      if(action==="comments"&&req.method==="GET"){
        const rows=await api(`comments?post_id=eq.${id}&select=*,profiles(username,display_name)&order=created_at.asc`,token);
        return reply({comments:(rows||[]).map((c:any)=>({id:c.id,body:c.body,author:{username:c.profiles?.username,displayName:c.profiles?.display_name}}))});
      }
      if(action==="comments"&&req.method==="POST"){
        const rows=await api("comments?select=*",token,{method:"POST",headers:{Prefer:"return=representation"},body:JSON.stringify([{post_id:id,user_id:user.id,body:String(body.body||"").trim()}])});
        return reply({comment:rows?.[0]},201);
      }
    }
    if (req.method==="GET" && path.startsWith("/api/users/") && !path.includes("/me")) {
      const parts=path.split("/").filter(Boolean);
      const name=decodeURIComponent(parts[2]||"").replace(/^@/,"").toLowerCase();
      if(parts[3]==="followers"||parts[3]==="following"){
        const target=(await api(`profiles?username=eq.${encodeURIComponent(name)}&select=id`,token))?.[0];
        if(!target)return reply({users:[]});
        const follower=parts[3]==="followers";
        const links=await api(`follows?${follower?"following_id":"follower_id"}=eq.${target.id}&select=*`,token);
        const ids=(links||[]).map((x:any)=>follower?x.follower_id:x.following_id);
        const users=ids.length?await api(`profiles?id=in.(${ids.join(",")})&select=id,username,display_name,bio,avatar_path,is_verified`,token):[];
        return reply({users:(users||[]).map((p:any)=>profileDto(p))});
      }
      if(parts[3]==="follow"&&req.method==="POST")return reply({error:"Rota inválida."},404);
      const rows=await api(`profiles?username=eq.${encodeURIComponent(name)}&select=*`,token);
      return rows?.[0]?reply({user:profileDto(rows[0])}):reply({error:"Perfil não encontrado."},404);
    }
    if(req.method==="POST"&&/^\/api\/users\/[0-9a-f-]+\/follow$/i.test(path)){
      const id=path.split("/")[3];
      const existing=await api(`follows?follower_id=eq.${user.id}&following_id=eq.${id}&select=follower_id`,token);
      if(existing?.length){await api(`follows?follower_id=eq.${user.id}&following_id=eq.${id}`,token,{method:"DELETE"});return reply({following:false});}
      await api("follows",token,{method:"POST",body:JSON.stringify([{follower_id:user.id,following_id:id}])});return reply({following:true});
    }
    if(req.method==="GET"&&path==="/api/users/search"){
      const q=url.searchParams.get("q")||"";
      const rows=await api(`profiles?or=(username.ilike.*${encodeURIComponent(q)}*,display_name.ilike.*${encodeURIComponent(q)}*)&select=id,username,display_name,bio,avatar_path,is_verified&limit=30`,token);
      return reply({users:(rows||[]).map((p:any)=>profileDto(p))});
    }
    if(req.method==="GET"&&path==="/api/notifications"){
      const rows=await api(`notifications?recipient_id=eq.${user.id}&select=*&order=created_at.desc&limit=100`,token);
      return reply({notifications:rows||[]});
    }
    if(req.method==="POST"&&path==="/api/notifications/read"){
      await api(`notifications?recipient_id=eq.${user.id}&read_at=is.null`,token,{method:"PATCH",body:JSON.stringify({read_at:new Date().toISOString()})});return reply({ok:true});
    }
    if(req.method==="GET"&&path==="/api/inbox"){
      const rows=await api(`messages?or=(sender_id.eq.${user.id},recipient_id.eq.${user.id})&select=*&order=created_at.desc&limit=200`,token);
      const latest=new Map<string,any>();
      for(const m of rows||[]){const peer=m.sender_id===user.id?m.recipient_id:m.sender_id;if(!latest.has(peer))latest.set(peer,m);}
      const ids=[...latest.keys()];
      const profiles=ids.length?await api(`profiles?id=in.(${ids.join(",")})&select=id,username,display_name,avatar_path`,token):[];
      return reply({conversations:[...latest.entries()].map(([id,m])=>{const p=(profiles||[]).find((x:any)=>x.id===id)||{};return {username:p.username,displayName:p.display_name,avatarUrl:p.avatar_path?`${SUPABASE_URL}/storage/v1/object/public/post-media/${p.avatar_path}`:"",body:m.body,createdAt:m.created_at,unreadCount:0};})});
    }
    const msg=path.match(/^\/api\/messages\/([^/]+)$/);
    if(msg){
      const peerName=decodeURIComponent(msg[1]).replace(/^@/,"").toLowerCase();
      const peer=(await api(`profiles?username=eq.${encodeURIComponent(peerName)}&select=id`,token))?.[0];
      if(!peer)return reply({messages:[]});
      if(req.method==="GET"){
        const rows=await api(`messages?or=(and(sender_id.eq.${user.id},recipient_id.eq.${peer.id}),and(sender_id.eq.${peer.id},recipient_id.eq.${user.id}))&select=*&order=created_at.asc&limit=500`,token);
        return reply({messages:(rows||[]).map((m:any)=>({id:m.id,body:m.body,senderId:m.sender_id,createdAt:m.created_at}))});
      }
      if(req.method==="POST"){
        const rows=await api("messages?select=*",token,{method:"POST",headers:{Prefer:"return=representation"},body:JSON.stringify([{sender_id:user.id,recipient_id:peer.id,body:String(body.body||"").trim()}])});
        return reply({message:rows?.[0]},201);
      }
    }
    if(req.method==="GET"&&path==="/api/users/me/export")return reply({error:"Exportação ainda indisponível nesta API migrada."},501);
    return reply({error:"Rota ainda não implementada no gateway Supabase.",path},404);
  } catch (e) {
    const detail = e instanceof Error ? e.message : String(e);
    console.error("VYBORA Supabase gateway:", detail);
    // Return the actual Supabase/PostgREST failure so the app can show the real reason.
    return reply({error: detail || "Erro desconhecido no gateway Supabase."},500);
  }
});
