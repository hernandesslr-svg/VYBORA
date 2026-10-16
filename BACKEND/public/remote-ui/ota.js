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
    if (showProgress) setStatus('Conectando ao servidor e verificando a versão publicada…');
    try {
      const response = await fetch('/ota/manifest.json?check=' + Date.now(), {
        method: 'GET',
        cache: 'no-store',
        headers: { Accept: 'application/json', 'Cache-Control': 'no-cache' }
      });
      if (!response.ok) throw new Error('O servidor respondeu com HTTP ' + response.status + '.');
      const manifest = await response.json();
      if (!manifest || typeof manifest.version !== 'string' || !manifest.version) {
        throw new Error('O servidor retornou um manifesto inválido.');
      }
      latestManifest = manifest;
      versionLabel.textContent = manifest.version;
      setStatus('Verificação concluída. Versão técnica publicada: ' + manifest.version + '.');
      return manifest;
    } catch (error) {
      setStatus('Falha na verificação: ' + (error && error.message ? error.message : 'erro desconhecido') + '.', true);
      throw error;
    } finally {
      busy = false;
      verifyButton.disabled = false;
      updateButton.disabled = false;
    }
  }

  verifyButton.addEventListener('click', () => {
    verify(true).catch(() => {});
  });

  updateButton.addEventListener('click', async () => {
    if (busy) return;
    updateButton.disabled = true;
    updateButton.textContent = 'Atualizando…';
    setStatus('Validando a versão publicada antes de recarregar…');
    try {
      const manifest = await verify(false);
      if (!manifest) throw new Error('Não foi possível obter a versão publicada.');
      setStatus('Servidor confirmado. Recarregando a interface atualizada…');
      const url = new URL(window.location.href);
      url.searchParams.set('refresh', String(Date.now()));
      url.searchParams.set('v', manifest.version);
      window.location.replace(url.toString());
    } catch (_) {
      updateButton.disabled = false;
      updateButton.textContent = 'Atualizar';
    }
  });

  // Executa uma verificação inicial para deixar claro que o JavaScript carregou.
  verify(true).catch(() => {});
})();