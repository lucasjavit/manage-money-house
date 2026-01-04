import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { configurationService } from '../services/configurationService';
import type { Configuration } from '../types';

const Settings = () => {
  const [openAIKey, setOpenAIKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    loadOpenAIKey();
  }, []);

  const loadOpenAIKey = async () => {
    try {
      const config = await configurationService.getConfiguration('openai.api.key');
      if (config) {
        setOpenAIKey(config.value || '');
      }
    } catch (error) {
      console.error('Error loading OpenAI key:', error);
    }
  };

  const handleSave = async () => {
    setLoading(true);
    setMessage(null);

    try {
      await configurationService.saveOrUpdateConfiguration({
        key: 'openai.api.key',
        value: openAIKey,
        description: 'API Key do OpenAI (ChatGPT) para uso futuro na aplicação',
      });

      setMessage({ type: 'success', text: 'API Key salva com sucesso!' });
    } catch (error) {
      console.error('Error saving OpenAI key:', error);
      setMessage({ type: 'error', text: 'Erro ao salvar API Key. Tente novamente.' });
    } finally {
      setLoading(false);
    }
  };

  const handleClear = () => {
    setOpenAIKey('');
  };

  return (
    <div className="max-w-2xl mx-auto p-6">
      <div className="bg-white/80 backdrop-blur-sm rounded-2xl shadow-lg border-2 border-slate-200/60 p-6">
        <div className="flex items-center gap-4 mb-6">
          <Link
            to="/"
            className="p-2 hover:bg-slate-100 rounded-lg transition-colors"
            title="Voltar"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5 text-slate-600"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M10 19l-7-7m0 0l7-7m-7 7h18"
              />
            </svg>
          </Link>
          <h2 className="text-2xl font-bold text-slate-800">Configurações</h2>
        </div>

        <div className="space-y-6">
          {/* OpenAI Configuration */}
          <div className="border-2 border-slate-200 rounded-xl p-5 bg-gradient-to-br from-slate-50 to-slate-100/50">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-gradient-to-br from-green-500 to-emerald-600 rounded-lg flex items-center justify-center">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-6 w-6 text-white"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"
                  />
                </svg>
              </div>
              <div>
                <h3 className="text-lg font-semibold text-slate-800">OpenAI API Key</h3>
                <p className="text-sm text-slate-600">
                  Configure sua chave da API do OpenAI (ChatGPT) para uso futuro
                </p>
              </div>
            </div>

            <div className="space-y-3">
              <div>
                <label htmlFor="openai-key" className="block text-sm font-semibold text-slate-700 mb-2">
                  API Key
                </label>
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
                    onClick={handleClear}
                    className="px-4 py-2 border-2 border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors font-semibold"
                  >
                    Limpar
                  </button>
                </div>
                <p className="text-xs text-slate-500 mt-2">
                  Sua chave será armazenada de forma segura no banco de dados
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
                className="w-full px-4 py-3 bg-gradient-to-r from-emerald-600 to-emerald-700 text-white rounded-lg hover:from-emerald-700 hover:to-emerald-800 transition-all disabled:opacity-50 disabled:cursor-not-allowed font-semibold shadow-md hover:shadow-lg"
              >
                {loading ? 'Salvando...' : 'Salvar API Key'}
              </button>
            </div>
          </div>

          {/* Info Box */}
          <div className="bg-blue-50 border-2 border-blue-200 rounded-xl p-4">
            <div className="flex gap-3">
              <div className="text-blue-600">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-6 w-6"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
              </div>
              <div className="flex-1">
                <h4 className="font-semibold text-blue-800 mb-1">Como obter sua API Key</h4>
                <ol className="text-sm text-blue-700 space-y-1 list-decimal list-inside">
                  <li>Acesse <a href="https://platform.openai.com/api-keys" target="_blank" rel="noopener noreferrer" className="underline font-semibold">platform.openai.com/api-keys</a></li>
                  <li>Faça login ou crie uma conta</li>
                  <li>Crie uma nova chave e copie aqui</li>
                </ol>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Settings;

