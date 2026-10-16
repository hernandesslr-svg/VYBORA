#!/usr/bin/env python3
"""Install the reviewed native CameraX capture screen into the Build 72 APK source."""
from pathlib import Path
import re
import sys

root = Path(sys.argv[1]).resolve()
workspace = Path(sys.argv[2]).resolve() if len(sys.argv) > 2 else root.parent
archive_main_files = list(root.rglob("MainActivity.kt"))
maintained_main = workspace / "app/src/main/java/app/vybora/android/MainActivity.kt"
api_files = list(root.rglob("VyboraApi.kt"))
if len(archive_main_files) != 1 or len(api_files) != 1 or not maintained_main.is_file():
    raise SystemExit(f"Camera patch needs one archived MainActivity, one VyboraApi and maintained source at {maintained_main}")

archive_main = archive_main_files[0]
source_main = maintained_main.read_text(encoding="utf-8")
target = archive_main.read_text(encoding="utf-8")
api_path = api_files[0]
api = api_path.read_text(encoding="utf-8")

def extract_function(text: str, signature: str, next_signature: str) -> str:
    start = text.find(signature)
    end = text.find(next_signature, start + len(signature))
    if start < 0 or end < 0:
        raise SystemExit(f"Could not extract camera function between {signature!r} and {next_signature!r}")
    return text[start:end].rstrip() + "\n\n"

camera_page = extract_function(
    source_main,
    "@Composable\nprivate fun PostPage(",
    "@Composable\nprivate fun LivePage("
)
old_start = target.find("@Composable\nprivate fun PostPage(")
old_end = target.find("@Composable\nprivate fun LivePage(", old_start + 1)
if old_start < 0 or old_end < 0:
    raise SystemExit("Could not locate archived PostPage/LivePage boundaries.")
target = target[:old_start] + camera_page + target[old_end:]

# Merge imports required by the maintained camera implementation into the archived
# Build 72 activity. The archive's imports are older and do not include CameraX UI symbols.
source_imports = [line for line in source_main.splitlines() if line.startswith("import ")]
target_lines = target.splitlines()
existing_imports = set(line for line in target_lines if line.startswith("import "))
missing_imports = [line for line in source_imports if line not in existing_imports]
if missing_imports:
    package_end = target.find("\n", target.find("package "))
    target = target[:package_end + 1] + "\n" + "\n".join(missing_imports) + "\n" + target[package_end + 1:]
    print(f"Applied: merged {len(missing_imports)} maintained Android imports")

# Build 72 calls PostPage using its legacy token argument; the maintained camera
# composable owns auth lookup and exposes onCameraCancelled instead. Match balanced
# parentheses because the legacy token expression itself contains nested calls.
call_count = 0
search_from = 0
while True:
    call_start = target.find("PostPage(", search_from)
    if call_start < 0:
        break
    depth = 0
    in_string = False
    escaped = False
    call_end = None
    for index in range(call_start + len("PostPage"), len(target)):
        ch = target[index]
        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
        elif ch == '(':
            depth += 1
        elif ch == ')':
            depth -= 1
            if depth == 0:
                call_end = index + 1
                break
    if call_end is None:
        raise SystemExit("Could not parse a PostPage call while adapting the Build 72 camera entry point.")
    invocation = target[call_start:call_end]
    if "token =" in invocation:
        target = target[:call_start] + 'PostPage(onNotice = { notice = it }, onCameraCancelled = { tab = 0 })' + target[call_end:]
        call_count = 1
        break
    search_from = call_end
if call_count:
    print("Applied: update Build 72 PostPage call to maintained camera API")
elif "onCameraCancelled" not in target:
    raise SystemExit("Could not adapt the Build 72 PostPage call to the maintained camera API.")
print("Applied: replace Build 72 post composer with maintained native CameraX photo/video screen")

# Keep the archived API client and Google Credential Manager intact; add adapters
# used by the maintained camera screen, backed by the existing signed-upload API.
api_anchor = "    private fun request(method: String, path: String, body: JSONObject? = null, token: String? = null): JSONObject {"
adapters = """    fun createTextPost(token: String, caption: String): JSONObject =
        request("POST", "/api/posts", JSONObject().put("caption", caption.trim()).put("mediaType", "text"), token)

    fun createMediaPost(token: String, caption: String, fileId: String, mediaType: String): JSONObject =
        request("POST", "/api/posts", JSONObject().put("caption", caption.trim())
            .put("mediaFileId", fileId).put("mediaType", mediaType), token)

    fun uploadPostMedia(context: android.content.Context, token: String, uri: Uri, fileName: String, contentType: String, sizeBytes: Long): String {
        val uploaded = uploadMedia(context.contentResolver, uri, token, "post_media")
        val id = uploaded.optString("id")
        if (id.isBlank()) throw IllegalStateException("O servidor não retornou o identificador da mídia enviada.")
        return id
    }

"""
if "fun createTextPost(token: String, caption: String)" not in api:
    if api.count(api_anchor) != 1:
        raise SystemExit("Could not locate VyboraApi request method for camera upload adapters.")
    api = api.replace(api_anchor, adapters + api_anchor, 1)
    print("Applied: camera publishing adapters for signed media upload")
api_path.write_text(api, encoding="utf-8")
archive_main.write_text(target, encoding="utf-8")

# Ensure video capture dependency and runtime microphone permission exist in the APK project.
gradle_candidates = list(root.rglob("app/build.gradle.kts"))
if len(gradle_candidates) != 1:
    raise SystemExit(f"Expected one app/build.gradle.kts; found {len(gradle_candidates)}")
gradle_path = gradle_candidates[0]
gradle = gradle_path.read_text(encoding="utf-8")
required_dependencies = [
    'androidx.camera:camera-core:1.4.2',
    'androidx.camera:camera-camera2:1.4.2',
    'androidx.camera:camera-lifecycle:1.4.2',
    'androidx.camera:camera-view:1.4.2',
    'androidx.camera:camera-video:1.4.2',
    'androidx.lifecycle:lifecycle-runtime-compose:2.8.7',
]
missing_dependencies = [
    f'    implementation("{coordinate}")'
    for coordinate in required_dependencies
    if coordinate not in gradle
]
if missing_dependencies:
    match = re.search(r'(?ms)^dependencies\s*\{(.*?)(^\})', gradle)
    if not match:
        raise SystemExit("Could not locate dependencies { ... } block to add camera dependencies.")
    gradle = gradle[:match.start(2)] + "\n".join(missing_dependencies) + "\n" + gradle[match.start(2):]
    gradle_path.write_text(gradle, encoding="utf-8")
    print("Applied camera/lifecycle dependencies:", ", ".join(line.strip() for line in missing_dependencies))

manifest_candidates = []
for p in root.rglob("AndroidManifest.xml"):
    try:
        t = p.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        continue
    if "<application" in t and "MainActivity" in t:
        manifest_candidates.append((p, t))
if not manifest_candidates:
    raise SystemExit("Could not find AndroidManifest.xml for camera permission.")
manifest_path, manifest = next(((p, t) for p, t in manifest_candidates if p.as_posix().endswith("app/src/main/AndroidManifest.xml")), manifest_candidates[0])
if 'android.permission.RECORD_AUDIO' not in manifest:
    anchor = '    <uses-permission android:name="android.permission.CAMERA" />'
    if manifest.count(anchor) != 1:
        raise SystemExit("Could not find CAMERA permission to add RECORD_AUDIO.")
    manifest = manifest.replace(anchor, anchor + '\n    <uses-permission android:name="android.permission.RECORD_AUDIO" />', 1)
    manifest_path.write_text(manifest, encoding="utf-8")
    print("Applied: runtime microphone permission declaration")

# Validate key features are in the exact source tree used for assembleRelease.
for required in ("VideoCapture.withOutput", "VideoRecordEvent.Finalize", 'captureMode by remember', 'Text("Efeitos"'):
    if required not in target:
        raise SystemExit(f"Camera source validation failed: missing {required!r}")
print("Camera source validation passed:", archive_main)
