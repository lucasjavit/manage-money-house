import { useEffect, useState } from 'react';
import type { RecommendedAsset, AssetAnalysis } from '../../types';
import { investmentService } from '../../services/investmentService';

interface AssetDetailModalProps {
  asset: RecommendedAsset;
  portfolioName: string;
  onClose: () => void;
}

const AssetDetailModal = ({ asset, portfolioName, onClose }: AssetDetailModalProps) => {
  const [analysis, setAnalysis] = useState<AssetAnalysis | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadAnalysis();
  }, [asset.ticker]);

  const loadAnalysis = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await investmentService.getAssetAnalysis(asset.ticker, portfolioName);
      setAnalysis(data);
    } catch (err) {
      console.error('Erro ao carregar análise:', err);
      setError('Não foi possível carregar a análise. Tente novamente.');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value: number | null | undefined) => {
    if (value === null || value === undefined) return '-';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  const getImpactColor = (impact: string | undefined) => {
    if (!impact) return 'text-gray-600';
    const lowerImpact = impact.toLowerCase();
    if (lowerImpact.startsWith('positivo')) return 'text-green-600';
    if (lowerImpact.startsWith('negativo')) return 'text-red-600';
    return 'text-yellow-600';
  };

  const getImpactBg = (impact: string | undefined) => {
    if (!impact) return 'bg-gray-50';
    const lowerImpact = impact.toLowerCase();
    if (lowerImpact.startsWith('positivo')) return 'bg-green-50';
    if (lowerImpact.startsWith('negativo')) return 'bg-red-50';
    return 'bg-yellow-50';
  };

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-hidden shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="bg-gradient-to-r from-indigo-600 to-purple-600 p-6">
          <div className="flex justify-between items-start">
            <div>
              <h2 className="text-2xl font-bold text-white">{asset.ticker}</h2>
              <p className="text-indigo-200">{asset.name}</p>
            </div>
            <button
              onClick={onClose}
              className="text-white hover:bg-white/20 p-2 rounded-lg transition-colors"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div className="flex gap-2 mt-3">
            <span className="bg-white/20 px-3 py-1 rounded-full text-sm text-white">
              {asset.type}
            </span>
            {asset.bias && asset.bias !== '-' && (
              <span
                className={`px-3 py-1 rounded-full text-sm font-semibold ${
                  asset.bias === 'Comprar'
                    ? 'bg-green-500 text-white'
                    : 'bg-yellow-500 text-white'
                }`}
              >
                {asset.bias}
              </span>
            )}
          </div>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto max-h-[calc(90vh-180px)] space-y-6">
          {loading ? (
            <div className="text-center py-16">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto" />
              <p className="mt-4 text-gray-600">Gerando analise com IA...</p>
              <p className="mt-2 text-sm text-gray-400">Isso pode levar alguns segundos</p>
            </div>
          ) : error ? (
            <div className="text-center py-16">
              <span className="text-5xl">⚠️</span>
              <p className="mt-4 text-red-600">{error}</p>
              <button
                onClick={loadAnalysis}
                className="mt-4 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
              >
                Tentar Novamente
              </button>
            </div>
          ) : (
            <>
              {/* Dados de Mercado */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                <div className="bg-gray-50 rounded-lg p-3 text-center">
                  <p className="text-xs text-gray-500 uppercase">Preco Atual</p>
                  <p className="text-lg font-bold text-gray-900">
                    {formatCurrency(analysis?.currentPrice ?? asset.currentPrice)}
                  </p>
                </div>
                <div className="bg-gray-50 rounded-lg p-3 text-center">
                  <p className="text-xs text-gray-500 uppercase">Preco-Teto</p>
                  <p className="text-lg font-bold text-gray-900">
                    {formatCurrency(analysis?.ceilingPrice ?? asset.ceilingPrice)}
                  </p>
                </div>
                <div className="bg-gray-50 rounded-lg p-3 text-center">
                  <p className="text-xs text-gray-500 uppercase">DY Esperado</p>
                  <p className="text-lg font-bold text-gray-900">
                    {(analysis?.expectedDY ?? asset.expectedDY) != null
                      ? `${analysis?.expectedDY ?? asset.expectedDY}%`
                      : '-'}
                  </p>
                </div>
                <div className="bg-gray-50 rounded-lg p-3 text-center">
                  <p className="text-xs text-gray-500 uppercase">Alocacao</p>
                  <p className="text-lg font-bold text-indigo-600">{asset.targetAllocation}%</p>
                </div>
              </div>

              {/* Análise Fundamentalista */}
              <div>
                <h3 className="font-semibold text-gray-800 mb-2 flex items-center gap-2">
                  <span>📊</span> Analise Fundamentalista
                </h3>
                <p className="text-gray-600 text-sm leading-relaxed bg-gray-50 rounded-lg p-4">
                  {analysis?.rationale || asset.rationale}
                </p>
              </div>

              {/* Análise IA */}
              {analysis?.aiAnalysis && (
                <div>
                  <h3 className="font-semibold text-gray-800 mb-2 flex items-center gap-2">
                    <span>🤖</span> Analise Detalhada (IA)
                  </h3>
                  <p className="text-gray-600 text-sm leading-relaxed bg-indigo-50 rounded-lg p-4">
                    {analysis.aiAnalysis}
                  </p>
                </div>
              )}

              {/* Por que investir */}
              {analysis?.investmentThesis && (
                <div>
                  <h3 className="font-semibold text-gray-800 mb-2 flex items-center gap-2">
                    <span>💡</span> Por que investir?
                  </h3>
                  <p className="text-gray-600 text-sm leading-relaxed bg-green-50 rounded-lg p-4">
                    {analysis.investmentThesis}
                  </p>
                </div>
              )}

              {/* Riscos */}
              {analysis?.risks && analysis.risks.length > 0 && (
                <div>
                  <h3 className="font-semibold text-gray-800 mb-2 flex items-center gap-2">
                    <span>⚠️</span> Principais Riscos
                  </h3>
                  <ul className="space-y-2">
                    {analysis.risks.map((risk, i) => (
                      <li
                        key={i}
                        className="flex items-start gap-2 text-sm text-gray-600 bg-red-50 rounded-lg p-3"
                      >
                        <span className="text-red-500 mt-0.5">•</span>
                        {risk}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Perspectiva */}
              {analysis?.shortTermOutlook && (
                <div>
                  <h3 className="font-semibold text-gray-800 mb-2 flex items-center gap-2">
                    <span>📈</span> Perspectiva (6-12 meses)
                  </h3>
                  <p className="text-gray-600 text-sm leading-relaxed bg-blue-50 rounded-lg p-4">
                    {analysis.shortTermOutlook}
                  </p>
                </div>
              )}

              {/* Comparação Setorial */}
              {analysis?.sectorComparison && (
                <div>
                  <h3 className="font-semibold text-gray-800 mb-2 flex items-center gap-2">
                    <span>🏢</span> Comparacao com Setor
                  </h3>
                  <p className="text-gray-600 text-sm leading-relaxed bg-purple-50 rounded-lg p-4">
                    {analysis.sectorComparison}
                  </p>
                </div>
              )}

              {/* Impacto Econômico */}
              {analysis?.economicImpact && (
                <div>
                  <h3 className="font-semibold text-gray-800 mb-3 flex items-center gap-2">
                    <span>📉</span> Impacto do Cenario Economico
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <div className={`rounded-lg p-4 ${getImpactBg(analysis.economicImpact.selicImpact)}`}>
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-sm font-medium text-gray-700">SELIC</span>
                        {analysis.economicImpact.selic && (
                          <span className="text-sm font-bold text-gray-900">
                            {analysis.economicImpact.selic}%
                          </span>
                        )}
                      </div>
                      <p className={`text-sm ${getImpactColor(analysis.economicImpact.selicImpact)}`}>
                        {analysis.economicImpact.selicImpact}
                      </p>
                    </div>
                    <div className={`rounded-lg p-4 ${getImpactBg(analysis.economicImpact.ipcaImpact)}`}>
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-sm font-medium text-gray-700">Inflacao (IPCA)</span>
                        {analysis.economicImpact.ipca && (
                          <span className="text-sm font-bold text-gray-900">
                            {analysis.economicImpact.ipca}%
                          </span>
                        )}
                      </div>
                      <p className={`text-sm ${getImpactColor(analysis.economicImpact.ipcaImpact)}`}>
                        {analysis.economicImpact.ipcaImpact}
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {/* Disclaimer */}
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mt-6">
                <p className="text-xs text-yellow-800">
                  <strong>Aviso:</strong> Esta analise e apenas educacional e nao constitui recomendacao
                  de investimento. Consulte um profissional certificado antes de investir.
                  Rentabilidade passada nao garante retornos futuros.
                </p>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default AssetDetailModal;
