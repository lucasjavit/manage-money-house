import React from 'react';
import type { PortfolioAnalysis } from '../../types';

interface PortfolioReviewModalProps {
  analyses: PortfolioAnalysis[];
  onClose: () => void;
  onReload: () => void;
  isLoading: boolean;
  status?: string;
}

const PortfolioReviewModal: React.FC<PortfolioReviewModalProps> = ({
  analyses,
  onClose,
  onReload,
  isLoading,
  status,
}) => {
  const formatDate = (dateString: string) => {
    try {
      return new Date(dateString).toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateString;
    }
  };

  const formatCurrency = (value: number | null) => {
    if (value === null) return '-';
    return value.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    });
  };

  const getRecommendationBadge = (recommendation: string) => {
    switch (recommendation) {
      case 'MANTER':
        return (
          <span className="px-2 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded-full">
            MANTER
          </span>
        );
      case 'SUBSTITUIR':
        return (
          <span className="px-2 py-1 bg-red-100 text-red-800 text-xs font-semibold rounded-full">
            SUBSTITUIR
          </span>
        );
      case 'OBSERVAR':
        return (
          <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-xs font-semibold rounded-full">
            OBSERVAR
          </span>
        );
      default:
        return (
          <span className="px-2 py-1 bg-gray-100 text-gray-800 text-xs font-semibold rounded-full">
            {recommendation}
          </span>
        );
    }
  };

  const getConfidenceColor = (score: number) => {
    if (score >= 80) return 'text-green-600';
    if (score >= 60) return 'text-yellow-600';
    return 'text-red-600';
  };

  const substitutions = analyses.filter((a) => a.recommendation === 'SUBSTITUIR');
  const observations = analyses.filter((a) => a.recommendation === 'OBSERVAR');
  const keepers = analyses.filter((a) => a.recommendation === 'MANTER');

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="bg-gradient-to-r from-indigo-600 to-purple-600 p-6">
          <div className="flex justify-between items-start">
            <div>
              <h2 className="text-2xl font-bold text-white">Analise de Carteiras</h2>
              <p className="text-indigo-200 mt-1">
                Revisao periodica dos ativos com IA
              </p>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={onReload}
                disabled={isLoading}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                  isLoading
                    ? 'bg-white/20 text-indigo-200 cursor-not-allowed'
                    : 'bg-white/20 text-white hover:bg-white/30'
                }`}
              >
                <svg
                  className={`w-5 h-5 ${isLoading ? 'animate-spin' : ''}`}
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                  />
                </svg>
                <span className="text-sm font-medium">
                  {isLoading ? 'Analisando...' : 'Rodar Nova Analise'}
                </span>
              </button>
              <button
                onClick={onClose}
                className="text-white hover:bg-white/20 p-2 rounded-lg transition-colors"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>

          {/* Stats */}
          {!isLoading && analyses.length > 0 && (
            <div className="grid grid-cols-4 gap-4 mt-4">
              <div className="bg-white/20 rounded-lg p-3 text-center">
                <p className="text-2xl font-bold text-white">{analyses.length}</p>
                <p className="text-xs text-indigo-200">Total Analisados</p>
              </div>
              <div className="bg-white/20 rounded-lg p-3 text-center">
                <p className="text-2xl font-bold text-green-300">{keepers.length}</p>
                <p className="text-xs text-indigo-200">Manter</p>
              </div>
              <div className="bg-white/20 rounded-lg p-3 text-center">
                <p className="text-2xl font-bold text-yellow-300">{observations.length}</p>
                <p className="text-xs text-indigo-200">Observar</p>
              </div>
              <div className="bg-white/20 rounded-lg p-3 text-center">
                <p className="text-2xl font-bold text-red-300">{substitutions.length}</p>
                <p className="text-xs text-indigo-200">Substituir</p>
              </div>
            </div>
          )}
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-20">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mb-4"></div>
              <p className="text-gray-600">{status || 'Analisando carteiras...'}</p>
              <p className="text-sm text-gray-500 mt-2">
                Isso pode levar alguns minutos
              </p>
            </div>
          ) : analyses.length === 0 ? (
            <div className="text-center py-20">
              <span className="text-6xl mb-4 block">📊</span>
              <p className="text-gray-600">Nenhuma analise encontrada.</p>
              <p className="text-sm text-gray-500 mt-2">
                Clique em "Rodar Analise" para iniciar.
              </p>
            </div>
          ) : (
            <div className="space-y-6">
              {/* Substituir Section */}
              {substitutions.length > 0 && (
                <div>
                  <h3 className="text-lg font-semibold text-red-700 mb-3 flex items-center gap-2">
                    <span className="w-3 h-3 bg-red-500 rounded-full"></span>
                    Recomendados para Substituicao ({substitutions.length})
                  </h3>
                  <div className="space-y-3">
                    {substitutions.map((analysis) => (
                      <AnalysisCard
                        key={analysis.id}
                        analysis={analysis}
                        formatDate={formatDate}
                        formatCurrency={formatCurrency}
                        getRecommendationBadge={getRecommendationBadge}
                        getConfidenceColor={getConfidenceColor}
                      />
                    ))}
                  </div>
                </div>
              )}

              {/* Observar Section */}
              {observations.length > 0 && (
                <div>
                  <h3 className="text-lg font-semibold text-yellow-700 mb-3 flex items-center gap-2">
                    <span className="w-3 h-3 bg-yellow-500 rounded-full"></span>
                    Em Observacao ({observations.length})
                  </h3>
                  <div className="space-y-3">
                    {observations.map((analysis) => (
                      <AnalysisCard
                        key={analysis.id}
                        analysis={analysis}
                        formatDate={formatDate}
                        formatCurrency={formatCurrency}
                        getRecommendationBadge={getRecommendationBadge}
                        getConfidenceColor={getConfidenceColor}
                      />
                    ))}
                  </div>
                </div>
              )}

              {/* Manter Section */}
              {keepers.length > 0 && (
                <div>
                  <h3 className="text-lg font-semibold text-green-700 mb-3 flex items-center gap-2">
                    <span className="w-3 h-3 bg-green-500 rounded-full"></span>
                    Manter na Carteira ({keepers.length})
                  </h3>
                  <div className="space-y-3">
                    {keepers.map((analysis) => (
                      <AnalysisCard
                        key={analysis.id}
                        analysis={analysis}
                        formatDate={formatDate}
                        formatCurrency={formatCurrency}
                        getRecommendationBadge={getRecommendationBadge}
                        getConfidenceColor={getConfidenceColor}
                      />
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

interface AnalysisCardProps {
  analysis: PortfolioAnalysis;
  formatDate: (date: string) => string;
  formatCurrency: (value: number | null) => string;
  getRecommendationBadge: (rec: string) => React.ReactNode;
  getConfidenceColor: (score: number) => string;
}

const AnalysisCard: React.FC<AnalysisCardProps> = ({
  analysis,
  formatDate,
  formatCurrency,
  getRecommendationBadge,
  getConfidenceColor,
}) => {
  const [expanded, setExpanded] = React.useState(false);

  return (
    <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
      {/* Header */}
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-3">
          <div>
            <span className="font-bold text-gray-900">{analysis.ticker}</span>
            <span className="text-gray-500 text-sm ml-2">{analysis.assetName}</span>
          </div>
          {getRecommendationBadge(analysis.recommendation)}
        </div>
        <div className="flex items-center gap-4 text-sm">
          <span className="text-gray-500">{analysis.portfolioName}</span>
          <span className={`font-semibold ${getConfidenceColor(analysis.confidenceScore)}`}>
            {analysis.confidenceScore}% confianca
          </span>
        </div>
      </div>

      {/* Price Info */}
      <div className="flex gap-6 text-sm mb-3">
        <div>
          <span className="text-gray-500">Atual: </span>
          <span className="font-medium">{formatCurrency(analysis.currentPrice)}</span>
        </div>
        <div>
          <span className="text-gray-500">Teto: </span>
          <span className="font-medium">{formatCurrency(analysis.ceilingPrice)}</span>
        </div>
        <div>
          <span className="text-gray-500">Tipo: </span>
          <span className="font-medium">{analysis.assetType}</span>
        </div>
      </div>

      {/* Expandable Analysis */}
      <button
        onClick={() => setExpanded(!expanded)}
        className="text-indigo-600 text-sm hover:text-indigo-800 flex items-center gap-1"
      >
        {expanded ? 'Ocultar analise' : 'Ver analise completa'}
        <svg
          className={`w-4 h-4 transition-transform ${expanded ? 'rotate-180' : ''}`}
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {expanded && (
        <div className="mt-3 pt-3 border-t border-gray-200 space-y-3">
          <div>
            <h4 className="text-sm font-semibold text-gray-700 mb-1">Analise:</h4>
            <p className="text-sm text-gray-600 whitespace-pre-wrap">{analysis.analysisText}</p>
          </div>

          {analysis.substitutionSuggestion && (
            <div className="bg-red-50 rounded-lg p-3">
              <h4 className="text-sm font-semibold text-red-700 mb-1">Sugestao de Substituicao:</h4>
              <p className="text-sm text-red-600">{analysis.substitutionSuggestion}</p>
            </div>
          )}

          <div className="text-xs text-gray-500 flex gap-4">
            <span>Analisado em: {formatDate(analysis.analysisDate)}</span>
            <span>Proxima revisao: {formatDate(analysis.nextReviewDate)}</span>
          </div>
        </div>
      )}
    </div>
  );
};

export default PortfolioReviewModal;
