(() => {
  "use strict";
  const $ = (selector, root = document) => root.querySelector(selector);
  const content = $("#page-content");
  const heading = $("#page-heading");
  const toastNode = $("#toast");
  const tokenKey = "vybora_token";
  let page = "feed";
  let manifestVersion = "";
  let manifestPoll = null;
  let authMode = "login";

  function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = String(text);
    return node;
  }
  function toast(message) {
    toastNode.textContent = message;
    toastNode.hidden = false;
    window.setTimeout(() => { toastNode.hidden = true; }, 3600);
  }
  function getToken() { return sessionStorage.getItem(tokenKey) || ""; }
  function setToken(token, username) {
    if (token) sessionStorage.setItem(tokenKey, token);
    if (window.VyboraNative && token) {
      try { window.VyboraNative.saveSession(token, username || ""); } catch (_) {}
    }
  }
  function clearToken() {
    sessionStorage.removeItem(tokenKey);
    if (window.VyboraNative) {
      try { window.VyboraNative.clearSession(); } catch (_) {}
    }
  }
  async function api(path, options = {}) {
    const headers = Object.assign({ Accept: "application/json" }, options.headers || {});
    if (options.body && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
    const token = getToken();
    if (token) headers.Authorization = "Bearer " + token;
    const response = await fetch(path, Object.assign({ cache: "no-store" }, options, { headers }));
    let data = {};
    try { data = await response.json(); } catch (_) {}
    if (!response.ok) throw new Error(data.error || ("Falha na API (HTTP " + response.status + ")."));
    return data;
  }
  function setHeading(title, subtitle, eyebrow) {
    heading.replaceChildren();
    const group = element("div");
    group.append(element("p", "eyebrow", eyebrow || "VYBORA"));
    group.append(element("h1", "", title));
    group.append(element("p", "muted", subtitle || ""));
    heading.append(group);
  }
  function avatar(name, url) {
    const node = element("div", "avatar");
    if (url && /^https:\/\//i.test(url)) {
      const img = element("img");
      img.src = url; img.alt = ""; img.referrerPolicy = "no-referrer";
      img.onerror = () => { node.replaceChildren(document.createTextNode((name || "V").slice(0, 1).toUpperCase())); };
      node.append(img);
    } else node.textContent = (name || "V").trim().slice(0, 1).toUpperCase() || "V";
    return node;
  }
  function showError(message) {
    const node = element("div", "empty-card error-text", message);
    content.prepend(node);
  }
  function makePostCard(post) {
    const card = element("article", "post-card");
    const head = element("div", "post-head");
    const author = post.author || {};
    const name = author.displayName || author.display_name || author.username || "VYBORA";
    head.append(avatar(name, author.avatarUrl || author.avatar_url));
    const authorText = element("div", "author");
    authorText.append(element("strong", "", name));
    authorText.append(element("small", "", author.username ? "@" + author.username : "Comunidade VYBORA"));
    head.append(authorText);
    card.append(head);
    if (post.caption) card.append(element("p", "post-caption", post.caption));
    const mediaUrl = typeof post.mediaUrl === "string" ? post.mediaUrl : "";
    if (mediaUrl && /^https:\/\//i.test(mediaUrl)) {
      if (String(post.mediaType).startsWith("image/") || post.mediaType === "image") {
        const image = element("img", "post-media");
        image.src = mediaUrl; image.alt = "Mídia da publicação"; image.loading = "lazy"; image.referrerPolicy = "no-referrer";
        card.append(image);
      } else if (String(post.mediaType).startsWith("video")) {
        const video = element("video", "post-media");
        video.src = mediaUrl; video.controls = true; video.preload = "metadata";
        card.append(video);
      }
    }
    const actions = element("div", "post-actions");
    const like = element("button", "text-button", "♡ " + Number(post.likes || 0) + " curtidas");
    like.type = "button";
    like.addEventListener("click", async () => {
      if (!getToken()) { go("profile"); toast("Entre na sua conta para curtir publicações."); return; }
      like.disabled = true;
      try { await api("/api/posts/" + encodeURIComponent(post.id) + "/like", { method: "POST", body: "{}" }); await renderFeed(); }
      catch (error) { toast(error.message); like.disabled = false; }
    });
    actions.append(like, element("span", "", "◌ " + Number(post.comments || 0) + " comentários"));
    card.append(actions);
    return card;
  }
  async function renderFeed() {
    page = "feed"; setHeading("Seu feed", "Publicações públicas da comunidade VYBORA.", "A COMUNIDADE");
    content.replaceChildren();
    if (getToken()) {
      const composer = element("form", "composer");
      const textarea = element("textarea");
      textarea.name = "caption"; textarea.maxLength = 5000; textarea.placeholder = "Compartilhe algo com a comunidade…"; textarea.setAttribute("aria-label", "Texto da publicação");
      const actions = element("div", "composer-actions");
      const submit = element("button", "primary-button", "Publicar texto"); submit.type = "submit";
      actions.append(submit); composer.append(textarea, actions);
      composer.addEventListener("submit", async event => {
        event.preventDefault();
        if (!textarea.value.trim()) { toast("Escreva algo antes de publicar."); return; }
        submit.disabled = true;
        try { await api("/api/posts", { method: "POST", body: JSON.stringify({ caption: textarea.value.trim(), mediaType: "text" }) }); toast("Publicação enviada."); await renderFeed(); }
        catch (error) { toast(error.message); submit.disabled = false; }
      });
      content.append(composer);
    }
    const list = element("div", "post-list");
    list.append(element("div", "loading-card", "Carregando publicações…")); content.append(list);
    try {
      const result = await api("/api/posts?limit=30");
      list.replaceChildren();
      const posts = Array.isArray(result.posts) ? result.posts : [];
      if (!posts.length) list.append(element("div", "empty-card", "Ainda não há publicações públicas. Seja a primeira pessoa a publicar."));
      else posts.forEach(post => list.append(makePostCard(post)));
    } catch (error) { list.replaceChildren(element("div", "empty-card error-text", error.message)); }
  }
  function renderUser(user) {
    const card = element("article", "user-card");
    const name = user.displayName || user.display_name || user.username || "Usuário";
    card.append(avatar(name, user.avatarUrl || user.avatar_url));
    const detail = element("div", "author");
    detail.append(element("strong", "", name));
    detail.append(element("small", "", "@" + (user.username || "")));
    if (user.bio) detail.append(element("small", "", user.bio));
    card.append(detail);
    const open = element("button", "secondary-button", "Ver perfil");
    open.type = "button";
    open.addEventListener("click", () => renderPublicProfile(user.username || ""));
    card.append(open);
    return card;
  }
  async function renderExplore() {
    page = "explore"; setHeading("Explorar", "Encontre pessoas e publicações.", "DESCOBERTA");
    content.replaceChildren();
    const form = element("form", "search-row");
    const input = element("input", "search-input");
    input.type = "search"; input.maxLength = 80; input.placeholder = "Pesquisar pessoas ou publicações"; input.setAttribute("aria-label", "Pesquisar");
    const submit = element("button", "primary-button", "Buscar"); submit.type = "submit";
    form.append(input, submit); content.append(form);
    const results = element("div", "user-list"); content.append(results);
    form.addEventListener("submit", async event => {
      event.preventDefault();
      const query = input.value.trim();
      if (query.length < 2) { toast("Digite pelo menos 2 caracteres."); return; }
      submit.disabled = true; results.replaceChildren(element("div", "loading-card", "Pesquisando…"));
      try {
        const data = await api("/api/search?q=" + encodeURIComponent(query));
        results.replaceChildren();
        (data.users || []).forEach(user => results.append(renderUser(user)));
        (data.posts || []).forEach(post => results.append(makePostCard(post)));
        if (!results.childElementCount) results.append(element("div", "empty-card", "Nenhum resultado encontrado."));
      } catch (error) { results.replaceChildren(element("div", "empty-card error-text", error.message)); }
      finally { submit.disabled = false; }
    });
    input.focus();
  }
  async function renderPublicProfile(username) {
    if (!username) { toast("Nome de usuário inválido."); return; }
    page = "explore"; setHeading("Perfil público", "Informações públicas da conta.", "COMUNIDADE");
    content.replaceChildren(element("div", "loading-card", "Carregando perfil…"));
    try {
      const result = await api("/api/users/" + encodeURIComponent(username));
      const user = result.user || {};
      const card = element("section", "post-card profile-card");
      card.append(avatar(user.display_name || user.username, user.avatar_url));
      card.lastChild.classList.add("profile-avatar");
      card.append(element("h2", "", user.display_name || user.username || "Usuário"));
      card.append(element("p", "muted", "@" + (user.username || "")));
      card.append(element("p", "", user.bio || "Esta pessoa ainda não adicionou uma biografia."));
      const stats = element("div", "profile-stats");
      [[user.posts_count || 0, "publicações"], [user.followers || 0, "seguidores"], [user.following || 0, "seguindo"]].forEach(pair => {
        const item = element("div"); item.append(element("strong", "", pair[0]), element("small", "", pair[1])); stats.append(item);
      });
      card.append(stats);
      const back = element("button", "secondary-button", "Voltar à busca");
      back.type = "button"; back.addEventListener("click", renderExplore); card.append(back);
      content.replaceChildren(card);
    } catch (error) { content.replaceChildren(element("div", "empty-card error-text", error.message)); }
  }
  function renderAuthForm() {
    content.replaceChildren();
    const card = element("section", "form-card");
    card.append(element("p", "eyebrow", "SUA CONTA"));
    card.append(element("h2", "", authMode === "login" ? "Entre no VYBORA" : "Crie sua conta"));
    card.append(element("p", "", "Esta interface experimental usa login por e-mail e senha. O login Google nativo continua disponível na tela original do aplicativo."));
    const form = element("form");
    if (authMode === "register") {
      const usernameLabel = element("label", "", "Nome de usuário");
      const username = element("input"); username.name = "username"; username.autocomplete = "username"; username.minLength = 3; username.maxLength = 24; username.required = true; username.pattern = "[a-zA-Z0-9_.]{3,24}";
      usernameLabel.append(username); form.append(usernameLabel);
    }
    const emailLabel = element("label", "", "E-mail");
    const email = element("input"); email.name = "email"; email.type = "email"; email.autocomplete = "email"; email.maxLength = 254; email.required = true;
    emailLabel.append(email); form.append(emailLabel);
    const passwordLabel = element("label", "", "Senha");
    const password = element("input"); password.name = "password"; password.type = "password"; password.autocomplete = authMode === "login" ? "current-password" : "new-password"; password.minLength = 8; password.maxLength = 72; password.required = true;
    passwordLabel.append(password); form.append(passwordLabel);
    const submit = element("button", "primary-button", authMode === "login" ? "Entrar" : "Criar conta"); submit.type = "submit"; form.append(submit);
    form.addEventListener("submit", async event => {
      event.preventDefault(); submit.disabled = true;
      const body = { email: email.value.trim(), password: password.value };
      if (authMode === "register") body.username = form.elements.username.value.trim().toLowerCase();
      try {
        const result = await api(authMode === "login" ? "/api/auth/login" : "/api/auth/register", { method: "POST", body: JSON.stringify(body) });
        if (!result.token) throw new Error("O servidor não retornou uma sessão.");
        setToken(result.token, result.user && result.user.username);
        toast("Sessão iniciada.");
        await renderProfile();
      } catch (error) { const message = element("p", "error-text", error.message); form.prepend(message); submit.disabled = false; }
    });
    card.append(form);
    const switcher = element("button", "text-button form-switch", authMode === "login" ? "Ainda não tem conta? Criar conta" : "Já tem conta? Entrar");
    switcher.type = "button"; switcher.addEventListener("click", () => { authMode = authMode === "login" ? "register" : "login"; renderAuthForm(); });
    card.append(switcher); content.append(card);
  }
  async function renderProfile() {
    page = "profile"; setHeading("Perfil e conta", "Sua conta, dados e sessão.", "VOCÊ");
    if (!getToken()) { renderAuthForm(); return; }
    content.replaceChildren(element("div", "loading-card", "Carregando sua conta…"));
    try {
      const result = await api("/api/auth/me");
      const user = result.user || {};
      const card = element("section", "post-card profile-card");
      card.append(avatar(user.displayName || user.username, user.avatarUrl));
      card.lastChild.classList.add("profile-avatar");
      card.append(element("h2", "", user.displayName || user.username || "Sua conta"));
      card.append(element("p", "muted", "@" + (user.username || "")));
      card.append(element("p", "", user.bio || "Sua biografia ainda está vazia."));
      const edit = element("button", "primary-button", "Editar nome e biografia"); edit.type = "button";
      edit.addEventListener("click", () => renderEditProfile(user)); card.append(edit);
      const logout = element("button", "secondary-button", "Sair desta conta"); logout.type = "button";
      logout.style.marginLeft = "8px";
      logout.addEventListener("click", () => { clearToken(); toast("Sessão encerrada."); renderProfile(); }); card.append(logout);
      content.replaceChildren(card);
    } catch (error) {
      clearToken(); renderAuthForm(); toast(error.message);
    }
  }
  function renderEditProfile(user) {
    setHeading("Editar perfil", "As alterações são salvas no backend.", "SUA CONTA");
    content.replaceChildren();
    const card = element("section", "form-card");
    const form = element("form");
    const fields = [
      ["displayName", "Nome de exibição", user.displayName || user.display_name || "", 80],
      ["username", "Nome de usuário", user.username || "", 24],
      ["bio", "Biografia", user.bio || "", 300]
    ];
    fields.forEach(([name, label, value, max]) => {
      const wrapper = element("label", "", label);
      const input = name === "bio" ? element("textarea") : element("input");
      input.name = name; input.value = value; input.maxLength = max; input.required = name === "username";
      if (name === "username") input.pattern = "[a-zA-Z0-9_.]{3,24}";
      if (name === "bio") { input.rows = 4; input.style.resize = "vertical"; }
      wrapper.append(input); form.append(wrapper);
    });
    const save = element("button", "primary-button", "Salvar perfil"); save.type = "submit"; form.append(save);
    form.addEventListener("submit", async event => {
      event.preventDefault(); save.disabled = true;
      try {
        await api("/api/users/me", { method: "PATCH", body: JSON.stringify({
          displayName: form.elements.displayName.value.trim(),
          username: form.elements.username.value.trim().toLowerCase(),
          bio: form.elements.bio.value.trim()
        }) });
        toast("Perfil atualizado."); await renderProfile();
      } catch (error) { toast(error.message); save.disabled = false; }
    });
    card.append(form);
    const back = element("button", "secondary-button", "Cancelar"); back.type = "button"; back.style.marginTop = "12px"; back.addEventListener("click", renderProfile);
    card.append(back); content.append(card);
  }
  async function go(next) {
    document.querySelectorAll(".nav-button").forEach(button => button.classList.toggle("active", button.dataset.page === next));
    if (next === "feed") await renderFeed();
    else if (next === "explore") await renderExplore();
    else await renderProfile();
  }
  async function checkManifest(initial) {
    try {
      const response = await fetch("/ota/manifest.json", { cache: "no-store", headers: { Accept: "application/json" } });
      if (!response.ok) throw new Error("Manifesto OTA indisponível.");
      const manifest = await response.json();
      const version = String(manifest.version || "");
      $("#ota-status").textContent = version ? "Interface v" + version : "OTA conectado";
      $("#version-label").textContent = "Versão da interface: " + (version || "desconhecida");
      manifestVersion = version;
      if (initial) {
        sessionStorage.setItem("vybora_ui_version", version);
        if (manifest.entrypoint !== "/remote-ui/") toast("Manifesto OTA validado; endereço de entrada inesperado.");
      } else if (version && sessionStorage.getItem("vybora_ui_version") !== version) {
        sessionStorage.setItem("vybora_ui_version", version);
        toast("Nova interface publicada. Atualizando…");
        window.setTimeout(() => window.location.reload(), 900);
      }
      return manifest;
    } catch (error) {
      $("#ota-status").textContent = "OTA offline";
      $("#version-label").textContent = "Atualização indisponível; tentando novamente.";
      throw error;
    }
  }
  async function boot() {
    document.querySelectorAll(".nav-button").forEach(button => button.addEventListener("click", () => go(button.dataset.page)));
    $("#refresh-button").addEventListener("click", async () => {
      try { await checkManifest(true); await go(page); toast("Interface atualizada."); }
      catch (error) { toast(error.message); }
    });
    window.addEventListener("vybora-native-session", () => { if (getToken()) renderProfile(); });
    try {
      await checkManifest(true);
      const config = await api("/api/app-config");
      $("#connection-state").textContent = "API conectada · versão de configuração " + (config.configVersion || 1);
      if (config.announcement) { $("#announcement").textContent = config.announcement; $("#announcement").hidden = false; }
      if (config.maintenanceMode) { $("#announcement").textContent = config.maintenanceMessage || "O VYBORA está em manutenção."; $("#announcement").hidden = false; }
    } catch (error) {
      $("#connection-state").textContent = "A API pode estar indisponível. " + error.message;
    }
    await go("feed");
    manifestPoll = window.setInterval(() => checkManifest(false).catch(() => {}), 60000);
  }
  window.addEventListener("pagehide", () => { if (manifestPoll) window.clearInterval(manifestPoll); });
  boot();
})();
