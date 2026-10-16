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

  window.vyboraOtaNativeStatus = (message, isError) => {
    setStatus(String(message || 'Atualização em andamento.'), Boolean(isError));
    if (!isError) updateButton.textContent = 'Atualizar';
    updateButton.disabled = !latestManifest;
  };

  async function verify(showProgress = true) {
    if (busy) return null;
    busy = true;
    verifyButton.disabled = true;
    updateButton.disabled = true;
    if (showProgress) setStatus('Verificando automaticamente se há uma versão nova…');
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
          !/github\.com$/i.test(new URL(manifest.apkUrl).hostname) ||
          !new URL(manifest.apkUrl).pathname.endsWith('/vybora-release.apk')) {
        throw new Error('O manifesto de APK não passou na validação.');
      }
      latestManifest = manifest;
      versionLabel.textContent = manifest.versionName;
      setStatus('Versão mais recente disponível: ' + manifest.versionName + '. Toque em Atualizar para baixar e abrir a instalação.');
      return manifest;
    } catch (error) {
      latestManifest = null;
      setStatus('Não foi possível verificar a atualização: ' + (error && error.message ? error.message : 'erro desconhecido'), true);
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
    updateButton.textContent = 'Iniciando download…';
    try {
      const manifest = latestManifest || await verify(true);
      if (!manifest) throw new Error('Não há um APK válido disponível.');
      if (window.VyboraNative && typeof window.VyboraNative.downloadAndInstall === 'function') {
        setStatus('Iniciando o download seguro pelo Android…');
        window.VyboraNative.downloadAndInstall(manifest.apkUrl);
      } else {
        setStatus('Este recurso de instalação integrada requer a versão atualizada do aplicativo. Abrindo o download como alternativa.', true);
        window.location.href = manifest.apkUrl;
      }
    } catch (error) {
      setStatus(error && error.message ? error.message : 'Não foi possível iniciar a atualização.', true);
      updateButton.disabled = !latestManifest;
    } finally {
      updateButton.textContent = 'Atualizar';
    }
  });

  updateButton.disabled = true;
  verify(true).catch(() => {});
})();
