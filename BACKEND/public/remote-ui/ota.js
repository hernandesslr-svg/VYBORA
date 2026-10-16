(() => {
  'use strict';
  const status = document.getElementById('ota-result');
  const verifyButton = document.getElementById('verify-update');
  const updateButton = document.getElementById('apply-update');
  const versionLabel = document.getElementById('ota-version');
  if (!status || !verifyButton || !updateButton || !versionLabel) return;
  let latestManifest = null;
  let busy = false;

  function setStatus(message, isError = false) {
    status.textContent = message;
    status.setAttribute('data-state', isError ? 'error' : 'ok');
  }

  async function verify(showProgress = true) {
    if (busy) return null;
    busy = true;
    verifyButton.disabled = true;
    updateButton.disabled = true;
    if (showProgress) setStatus('Consultando a versão APK publicada…');
    try {
      const response = await fetch('/api/app-update?check=' + Date.now(), {
        method: 'GET',
        cache: 'no-store',
        headers: { Accept: 'application/json', 'Cache-Control': 'no-cache' }
      });
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.error || 'O servidor respondeu com HTTP ' + response.status + '.');
      }
      const manifest = await response.json();
      if (manifest.appId !== 'app.vybora.android' ||
          !Number.isSafeInteger(manifest.versionCode) ||
          manifest.versionCode < 1 ||
          typeof manifest.versionName !== 'string' ||
          !/^https:\/\//i.test(manifest.apkUrl || '') ||
          !/github\.com$/i.test(new URL(manifest.apkUrl).hostname)) {
        throw new Error('O manifesto de APK não passou na validação.');
      }
      latestManifest = manifest;
      versionLabel.textContent = manifest.versionName;
      setStatus('APK publicado: versão ' + manifest.versionName + ' (código ' + manifest.versionCode + ').');
      return manifest;
    } catch (error) {
      latestManifest = null;
      setStatus('Não foi possível verificar o APK: ' + (error && error.message ? error.message : 'erro desconhecido'), true);
      throw error;
    } finally {
      busy = false;
      verifyButton.disabled = false;
      updateButton.disabled = !latestManifest;
    }
  }

  verifyButton.addEventListener('click', () => verify(true).catch(() => {}));
  updateButton.addEventListener('click', async () => {
    if (busy) return;
    updateButton.disabled = true;
    updateButton.textContent = 'Abrindo download…';
    try {
      const manifest = latestManifest || await verify(true);
      if (!manifest) throw new Error('Verifique a versão antes de continuar.');
      setStatus('Abrindo o download do APK. Após baixar, toque no arquivo e confirme a instalação do Android.');
      window.location.href = manifest.apkUrl;
    } catch (_) {
      updateButton.disabled = !latestManifest;
    } finally {
      updateButton.textContent = 'Atualizar';
    }
  });
  updateButton.disabled = true;
  verify(true).catch(() => {});
})();