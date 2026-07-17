import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { configurationService } from '../services/configurationService';

type Provider = 'openai' | 'anthropic';

const Settings = () => {
  const [provider, setProvider] = useState<Provider>('openai');
  const [openAIKey, setOpenAIKey] = useState('');
  const [anthropicKey, setAnthropicKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    try {
      const [openai, anthropic, aiProvider] = await Promise.all([
        configurationService.getConfiguration('openai.api.key'),
        configurationService.getConfiguration('anthropic.api.key'),
        configurationService.getConfiguration('ai.provider'),
      ]);
      if (openai) setOpenAIKey(openai.value || '');
      if (anthropic) setAnthropicKey(anthropic.value || '');
      if (aiProvider?.value === 'anthropic') setProvider('anthropic');
    } catch (error) {
      console.error('Error loading settings:', error);
    }
  };

  const handleSave = async () => {
    setLoading(true);
    setMessage(null);

    try {
      await configurationService.saveOrUpdateConfiguration({
        key: 'ai.provider',
        value: provider,
        description: 'Provider de IA usado pela aplicação: openai ou anthropic',
      });
      await configurationService.saveOrUpdateConfiguration({
        key: 'openai.api.key',
        value: openAIKey,
        description: 'API Key do OpenAI (ChatGPT)',
      });
      await configurationService.saveOrUpdateConfiguration({
        key: 'anthropic.api.key',
        value: anthropicKey,
        description: 'API Key da Anthropic (Claude)',
      });

      setMessage({ type: 'success', text: 'Configurações salvas com sucesso!' });
    } catch (error) {
      console.error('Error saving settings:', error);
      setMessage({ type: 'error', text: 'Erro ao salvar. Tente novamente.' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto p-6">
      <div className="bg-white/80 backdrop-blur-sm rounded-2xl shadow-lg border-2 border-slate-200/60 p-6">
        <div className="flex items-center gap-4 mb-6">
          <Link to="/" className="p-2 hover:bg-slate-100 rounded-lg transition-colors" title="Voltar">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-slate-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
          </Link>
          <h2 className="text-2xl font-bold text-slate-800">Configurações</h2>
        </div>

        <div className="space-y-6">
          {/* Provider de IA */}
          <div className="border-2 border-slate-200 rounded-xl p-5 bg-gradient-to-br from-slate-50 to-slate-100/50">
            <h3 className="text-lg font-semibold text-slate-800 mb-1">Provider de IA</h3>
            <p className="text-sm text-slate-600 mb-4">
              Qual serviço a aplicação usa nas análises de IA (insights, portfólio, relatórios B3).
            </p>
            <div className="grid grid-cols-2 gap-3">
              {(
                [
                  { id: 'openai', label: 'OpenAI', hint: 'ChatGPT' },
                  { id: 'anthropic', label: 'Claude', hint: 'Anthropic' },
                ] as { id: Provider; label: string; hint: string }[]
              ).map((opt) => (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => setProvider(opt.id)}
                  className={`px-4 py-3 rounded-lg border-2 text-left transition-all ${
                    provider === opt.id
                      ? 'border-emerald-500 bg-emerald-50 ring-2 ring-emerald-500/20'
                      : 'border-slate-300 bg-white hover:border-slate-400'
                  }`}
                >
                  <span className="block text-sm font-bold text-slate-800">{opt.label}</span>
                  <span className="block text-xs text-slate-500">{opt.hint}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Anthropic (Claude) */}
          <div
            className={`border-2 rounded-xl p-5 transition-all ${
              provider === 'anthropic'
                ? 'border-orange-300 bg-gradient-to-br from-orange-50 to-amber-50/50'
                : 'border-slate-200 bg-gradient-to-br from-slate-50 to-slate-100/50'
            }`}
          >
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-lg font-semibold text-slate-800">Anthropic API Key (Claude)</h3>
                <p className="text-sm text-slate-600">Chave da API da Anthropic, cobrada por uso.</p>
              </div>
              {provider === 'anthropic' && (
                <span className="text-[10px] font-bold uppercase text-orange-700 bg-orange-100 px-2 py-1 rounded">
                  Em uso
                </span>
              )}
            </div>
            <div className="flex gap-2">
              <input
                id="anthropic-key"
                type="password"
                value={anthropicKey}
                onChange={(e) => setAnthropicKey(e.target.value)}
                placeholder="sk-ant-..."
                className="flex-1 px-4 py-2 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500 transition-all bg-white text-slate-800 font-mono text-sm"
              />
              <button
                onClick={() => setAnthropicKey('')}
                className="px-4 py-2 border-2 border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors font-semibold"
              >
                Limpar
              </button>
            </div>
            <p className="text-xs text-slate-500 mt-2">
              Crie em{' '}
              <a
                href="https://console.anthropic.com/settings/keys"
                target="_blank"
                rel="noopener noreferrer"
                className="underline font-semibold"
              >
                console.anthropic.com
              </a>
              . É uma chave da API (paga por uso), diferente da assinatura do Claude Code.
            </p>
          </div>

          {/* OpenAI */}
          <div
            className={`border-2 rounded-xl p-5 transition-all ${
              provider === 'openai'
                ? 'border-emerald-300 bg-gradient-to-br from-emerald-50 to-green-50/50'
                : 'border-slate-200 bg-gradient-to-br from-slate-50 to-slate-100/50'
            }`}
          >
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-lg font-semibold text-slate-800">OpenAI API Key</h3>
                <p className="text-sm text-slate-600">Chave da API do OpenAI (ChatGPT).</p>
              </div>
              {provider === 'openai' && (
                <span className="text-[10px] font-bold uppercase text-emerald-700 bg-emerald-100 px-2 py-1 rounded">
                  Em uso
                </span>
              )}
            </div>
            <div className="flex gap-2">
              <input
                id="openai-key"
                type="password"
                value={openAIKey}
                onChange={(e) => setOpenAIKey(e.target.value)}
                placeholder="sk-..."
                className="flex-1 px-4 py-2 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 transition-all bg-white text-slate-800 font-mono text-sm"
              />
              <button
                onClick={() => setOpenAIKey('')}
                className="px-4 py-2 border-2 border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors font-semibold"
              >
                Limpar
              </button>
            </div>
            <p className="text-xs text-slate-500 mt-2">
              Crie em{' '}
              <a
                href="https://platform.openai.com/api-keys"
                target="_blank"
                rel="noopener noreferrer"
                className="underline font-semibold"
              >
                platform.openai.com/api-keys
              </a>
              .
            </p>
          </div>

          {message && (
            <div
              className={`p-3 rounded-lg border-2 ${
                message.type === 'success'
                  ? 'bg-emerald-50 border-emerald-200 text-emerald-700'
                  : 'bg-red-50 border-red-200 text-red-700'
              }`}
            >
              {message.text}
            </div>
          )}

          <button
            onClick={handleSave}
            disabled={loading}
            className="w-full px-4 py-3 bg-gradient-to-r from-blue-600 to-blue-700 text-white rounded-lg hover:from-blue-700 hover:to-blue-800 transition-all disabled:opacity-50 disabled:cursor-not-allowed font-semibold shadow-md hover:shadow-lg"
          >
            {loading ? 'Salvando...' : 'Salvar configurações'}
          </button>

          {/* Aviso: extrato/boleto/salário seguem no OpenAI */}
          <div className="bg-blue-50 border-2 border-blue-200 rounded-xl p-4">
            <div className="flex gap-3">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-blue-600 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div className="flex-1 text-sm text-blue-700">
                <h4 className="font-semibold text-blue-800 mb-1">O que usa o provider selecionado</h4>
                <p>
                  AI Insights, análises de portfólio e relatórios da B3 usam o provider escolhido acima.
                  A leitura de extratos, boletos e salários continua usando o OpenAI — mantenha a chave
                  do OpenAI preenchida se usar esses recursos.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Settings;
