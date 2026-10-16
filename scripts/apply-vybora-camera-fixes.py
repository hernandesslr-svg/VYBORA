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
if 'androidx.camera:camera-video:' not in gradle:
    anchor = 'implementation("androidx.camera:camera-core:1.4.2")'
    if gradle.count(anchor) != 1:
        raise SystemExit("Could not find CameraX core dependency to add CameraX video.")
    gradle = gradle.replace(anchor, anchor + '\n    implementation("androidx.camera:camera-video:1.4.2")', 1)
    gradle_path.write_text(gradle, encoding="utf-8")
    print("Applied: CameraX VideoCapture dependency")

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
for required in ("VideoCapture.withOutput", "VideoRecordEvent.Finalize", 'captureMode by remember', 'FILTROS AO VIVO'):
    if required not in target:
        raise SystemExit(f"Camera source validation failed: missing {required!r}")
print("Camera source validation passed:", archive_main)
