import React, { useState, useEffect, useRef } from 'react';
import ReactDOM from 'react-dom';
import type { RealPortfolioSummary, RealPortfolioPosition, RealPortfolioDividend, HealthScoreDetails } from '../../types';
import { realPortfolioService } from '../../services/realPortfolioService';

// Componente de Tooltip com Portal para renderizar fora da hierarquia do DOM
interface TooltipPortalProps {
  position: RealPortfolioPosition;
  anchorRef: React.RefObject<HTMLDivElement>;
  isVisible: boolean;
}

const TooltipPortal: React.FC<TooltipPortalProps> = ({ position, anchorRef, isVisible }) => {
  const [coords, setCoords] = useState({ top: 0, left: 0 });

  useEffect(() => {
    if (isVisible && anchorRef.current) {
      const rect = anchorRef.current.getBoundingClientRect();
      setCoords({
        top: rect.top - 10, // Um pouco acima do elemento
        left: Math.min(rect.right - 320, window.innerWidth - 340), // 320px de largura + margem
      });
    }
  }, [isVisible, anchorRef]);

  if (!isVisible || !position.aiAnalysis) return null;

  return ReactDOM.createPortal(
    <div
      className="fixed z-[99999] w-80 p-4 bg-white rounded-lg shadow-2xl border border-gray-200 text-left pointer-events-none"
      style={{
        top: coords.top,
        left: coords.left,
        transform: 'translateY(-100%)',
      }}
    >
      <div className="flex items-center gap-2 mb-2">
        <span className={`px-2 py-1 rounded text-xs font-bold border ${getRecommendationColor(position.aiRecommendation)}`}>
          {position.aiRecommendation}
        </span>
        {position.aiConfidenceScore && (
          <span className="text-xs text-gray-500">
            Confianca: {(position.aiConfidenceScore * 100).toFixed(0)}%
          </span>
        )}
      </div>
      {position.aiMainReason && (
        <p className="text-sm font-medium text-gray-800 mb-1">
          {position.aiMainReason}
        </p>
      )}
      <p className="text-xs text-gray-600">
        {position.aiAnalysis}
      </p>
      <div className="mt-2 pt-2 border-t border-gray-100 flex justify-between text-xs text-gray-400">
        <span className={getRiskColor(position.aiRiskLevel)}>
          Risco: {position.aiRiskLevel || '-'}
        </span>
        <span>
          {position.aiAnalyzedAt ? formatDateForTooltip(position.aiAnalyzedAt) : '-'}
        </span>
      </div>
    </div>,
    document.body
  );
};

const formatDateForTooltip = (dateStr: string): string => {
  try {
    const date = new Date(dateStr);
    return date.toLocaleDateString('pt-BR');
  } catch {
    return dateStr;
  }
};

// Componente de celula com tooltip
interface ActionCellProps {
  position: RealPortfolioPosition;
}

const ActionCell: React.FC<ActionCellProps> = ({ position }) => {
  const [isHovered, setIsHovered] = useState(false);
  const anchorRef = useRef<HTMLDivElement>(null);

  return (
    <td className="px-4 py-2 text-center">
      <div
        ref={anchorRef}
        className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium border cursor-help ${getRecommendationColor(position.aiRecommendation)}`}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
      >
        <span>{getRecommendationIcon(position.aiRecommendation)}</span>
        <span>{getRecommendationLabel(position.aiRecommendation)}</span>
      </div>
      <TooltipPortal position={position} anchorRef={anchorRef} isVisible={isHovered} />
    </td>
  );
};

// Componente de celula de Vies com tooltip
const BiasCell: React.FC<ActionCellProps> = ({ position }) => {
  const [isHovered, setIsHovered] = useState(false);
  const anchorRef = useRef<HTMLDivElement>(null);

  return (
    <td className="px-4 py-2 text-center">
      <div
        ref={anchorRef}
        className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium border cursor-help ${getBiasColor(position.aiBias)}`}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
      >
        {getBiasLabel(position.aiBias)}
      </div>
      <TooltipPortal position={position} anchorRef={anchorRef} isVisible={isHovered} />
    </td>
  );
};

interface Props {
  portfolio: RealPortfolioSummary;
  onNewUpload: () => void;
  onRefresh: () => void;
}

const formatCurrency = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return 'R$ 0,00';
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatNumber = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return '0';
  return new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 4,
  }).format(value);
};

const formatDate = (dateStr: string | null | undefined): string => {
  if (!dateStr) return '-';
  try {
    const date = new Date(dateStr);
    return date.toLocaleDateString('pt-BR');
  } catch {
    return dateStr;
  }
};

// Funcoes auxiliares para recomendacoes
const getRecommendationColor = (rec: string | undefined) => {
  switch (rec) {
    case 'MANTER': return 'bg-green-100 text-green-800 border-green-300';
    case 'COMPRAR_MAIS': return 'bg-blue-100 text-blue-800 border-blue-300';
    case 'VENDER': return 'bg-red-100 text-red-800 border-red-300';
    default: return 'bg-gray-100 text-gray-600 border-gray-300';
  }
};

const getRecommendationBgColor = (rec: string | undefined) => {
  switch (rec) {
    case 'MANTER': return 'bg-green-50';
    case 'COMPRAR_MAIS': return 'bg-blue-50';
    case 'VENDER': return 'bg-red-50';
    default: return '';
  }
};

const getRecommendationIcon = (rec: string | undefined) => {
  switch (rec) {
    case 'MANTER': return '✓';
    case 'COMPRAR_MAIS': return '↑';
    case 'VENDER': return '↓';
    default: return '?';
  }
};

const getRecommendationLabel = (rec: string | undefined) => {
  switch (rec) {
    case 'MANTER': return 'Manter';
    case 'COMPRAR_MAIS': return 'Comprar';
    case 'VENDER': return 'Vender';
    default: return 'Analisar';
  }
};

const getRiskColor = (risk: string | undefined) => {
  switch (risk) {
    case 'BAIXO': return 'text-green-600';
    case 'MEDIO': return 'text-yellow-600';
    case 'ALTO': return 'text-red-600';
    default: return 'text-gray-600';
  }
};

const getBiasColor = (bias: string | undefined) => {
  switch (bias) {
    case 'COMPRA': return 'bg-green-100 text-green-800 border-green-300';
    case 'VENDA': return 'bg-red-100 text-red-800 border-red-300';
    case 'NEUTRO': return 'bg-gray-100 text-gray-700 border-gray-300';
    default: return 'bg-gray-50 text-gray-500 border-gray-200';
  }
};

const getBiasLabel = (bias: string | undefined) => {
  switch (bias) {
    case 'COMPRA': return 'Compra';
    case 'VENDA': return 'Venda';
    case 'NEUTRO': return 'Neutro';
    default: return '-';
  }
};

// Funcoes auxiliares para Health Score
const getHealthScoreColor = (score: number | undefined) => {
  if (!score) return { bg: 'bg-gray-100', text: 'text-gray-600', ring: 'ring-gray-300' };
  if (score >= 85) return { bg: 'bg-green-100', text: 'text-green-700', ring: 'ring-green-400' };
  if (score >= 70) return { bg: 'bg-blue-100', text: 'text-blue-700', ring: 'ring-blue-400' };
  if (score >= 50) return { bg: 'bg-yellow-100', text: 'text-yellow-700', ring: 'ring-yellow-400' };
  if (score >= 30) return { bg: 'bg-orange-100', text: 'text-orange-700', ring: 'ring-orange-400' };
  return { bg: 'bg-red-100', text: 'text-red-700', ring: 'ring-red-400' };
};

const getStatusLabel = (status: string | undefined) => {
  switch (status) {
    case 'EXCELENTE': return { label: 'Excelente', emoji: '🌟' };
    case 'BOM': return { label: 'Bom', emoji: '👍' };
    case 'REGULAR': return { label: 'Regular', emoji: '⚠️' };
    case 'RUIM': return { label: 'Ruim', emoji: '⚡' };
    case 'CRITICO': return { label: 'Critico', emoji: '🚨' };
    default: return { label: 'Analisando', emoji: '⏳' };
  }
};

// Componente de Health Score com Tooltip
interface HealthScoreCardProps {
  healthScore?: number;
  healthScoreDetails?: HealthScoreDetails;
}

const HealthScoreTooltipPortal: React.FC<{
  details: HealthScoreDetails;
  anchorRef: React.RefObject<HTMLDivElement>;
  isVisible: boolean;
}> = ({ details, anchorRef, isVisible }) => {
  const [coords, setCoords] = useState({ top: 0, left: 0 });

  useEffect(() => {
    if (isVisible && anchorRef.current) {
      const rect = anchorRef.current.getBoundingClientRect();
      setCoords({
        top: rect.bottom + 10,
        left: Math.min(rect.left, window.innerWidth - 380),
      });
    }
  }, [isVisible, anchorRef]);

  if (!isVisible) return null;

  return ReactDOM.createPortal(
    <div
      className="fixed z-[99999] w-[360px] p-5 bg-white rounded-xl shadow-2xl border border-gray-200 text-left pointer-events-none"
      style={{
        top: coords.top,
        left: coords.left,
      }}
    >
      {/* Header com status */}
      <div className="flex items-center justify-between mb-4 pb-3 border-b border-gray-100">
        <h4 className="font-bold text-gray-800">Saude da Carteira</h4>
        <span className={`text-sm font-medium px-2 py-1 rounded-full ${getHealthScoreColor(details.diversificationScore + details.concentrationScore + details.qualityScore + details.riskScore).bg}`}>
          {getStatusLabel(details.overallStatus).emoji} {getStatusLabel(details.overallStatus).label}
        </span>
      </div>

      {/* Scores detalhados */}
      <div className="space-y-3 mb-4">
        <div>
          <div className="flex justify-between text-sm mb-1">
            <span className="text-gray-600">Diversificacao</span>
            <span className="font-medium">{details.diversificationScore?.toFixed(1) || 0}/25</span>
          </div>
          <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
            <div className="h-full bg-blue-500 transition-all" style={{ width: `${(details.diversificationScore || 0) * 4}%` }}></div>
          </div>
        </div>

        <div>
          <div className="flex justify-between text-sm mb-1">
            <span className="text-gray-600">Concentracao</span>
            <span className="font-medium">{details.concentrationScore?.toFixed(1) || 0}/25</span>
          </div>
          <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
            <div className="h-full bg-purple-500 transition-all" style={{ width: `${(details.concentrationScore || 0) * 4}%` }}></div>
          </div>
        </div>

        <div>
          <div className="flex justify-between text-sm mb-1">
            <span className="text-gray-600">Qualidade dos Ativos</span>
            <span className="font-medium">{details.qualityScore?.toFixed(1) || 0}/25</span>
          </div>
          <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
            <div className="h-full bg-green-500 transition-all" style={{ width: `${(details.qualityScore || 0) * 4}%` }}></div>
          </div>
        </div>

        <div>
          <div className="flex justify-between text-sm mb-1">
            <span className="text-gray-600">Nivel de Risco</span>
            <span className="font-medium">{details.riskScore?.toFixed(1) || 0}/25</span>
          </div>
          <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
            <div className="h-full bg-amber-500 transition-all" style={{ width: `${(details.riskScore || 0) * 4}%` }}></div>
          </div>
        </div>
      </div>

      {/* Ponto forte e fraco */}
      <div className="space-y-2 mb-4 pt-3 border-t border-gray-100">
        <div className="flex items-start gap-2">
          <span className="text-green-500 text-lg">✓</span>
          <div>
            <span className="text-xs font-medium text-gray-500">Ponto Forte</span>
            <p className="text-sm text-gray-700">{details.mainStrength}</p>
          </div>
        </div>
        <div className="flex items-start gap-2">
          <span className="text-amber-500 text-lg">!</span>
          <div>
            <span className="text-xs font-medium text-gray-500">Ponto de Atencao</span>
            <p className="text-sm text-gray-700">{details.mainWeakness}</p>
          </div>
        </div>
      </div>

      {/* Recomendacoes */}
      {details.recommendations && details.recommendations.length > 0 && (
        <div className="pt-3 border-t border-gray-100">
          <span className="text-xs font-medium text-gray-500">Recomendacoes</span>
          <ul className="mt-1 space-y-1">
            {details.recommendations.slice(0, 3).map((rec, idx) => (
              <li key={idx} className="text-xs text-gray-600 flex items-start gap-1">
                <span className="text-blue-500">•</span>
                <span>{rec}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>,
    document.body
  );
};

const HealthScoreCard: React.FC<HealthScoreCardProps> = ({ healthScore, healthScoreDetails }) => {
  const [isHovered, setIsHovered] = useState(false);
  const anchorRef = useRef<HTMLDivElement>(null);

  const colors = getHealthScoreColor(healthScore);
  const status = getStatusLabel(healthScoreDetails?.overallStatus);

  return (
    <div
      ref={anchorRef}
      className={`relative bg-white rounded-xl p-5 border-2 ${colors.ring.replace('ring', 'border')} cursor-help transition-all hover:shadow-lg`}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-sm font-medium text-gray-600 mb-1">Saude da Carteira</h3>
          <div className="flex items-center gap-2">
            <span className="text-3xl font-bold text-gray-800">
              {healthScore != null ? healthScore.toFixed(0) : '--'}
            </span>
            <span className="text-lg text-gray-400">/100</span>
          </div>
          {healthScoreDetails && (
            <div className="flex items-center gap-1 mt-1">
              <span className="text-lg">{status.emoji}</span>
              <span className={`text-sm font-medium ${colors.text}`}>{status.label}</span>
            </div>
          )}
        </div>

        {/* Circular Progress */}
        <div className="relative w-20 h-20">
          <svg className="w-20 h-20 transform -rotate-90" viewBox="0 0 100 100">
            <circle
              className="text-gray-200"
              strokeWidth="8"
              stroke="currentColor"
              fill="transparent"
              r="40"
              cx="50"
              cy="50"
            />
            <circle
              className={colors.text.replace('text', 'stroke').replace('-700', '-500')}
              strokeWidth="8"
              strokeDasharray={`${(healthScore || 0) * 2.51} 251.2`}
              strokeLinecap="round"
              stroke="currentColor"
              fill="transparent"
              r="40"
              cx="50"
              cy="50"
            />
          </svg>
          <div className="absolute inset-0 flex items-center justify-center">
            <span className={`text-sm font-bold ${colors.text}`}>
              {healthScore != null ? `${healthScore.toFixed(0)}%` : '--'}
            </span>
          </div>
        </div>
      </div>

      {!healthScoreDetails && (
        <p className="text-xs text-gray-400 mt-2">
          Clique em "Analisar Ativos" para calcular o score
        </p>
      )}

      {healthScoreDetails && (
        <p className="text-xs text-gray-400 mt-2">
          Passe o mouse para ver detalhes
        </p>
      )}

      {/* Tooltip */}
      {healthScoreDetails && (
        <HealthScoreTooltipPortal
          details={healthScoreDetails}
          anchorRef={anchorRef}
          isVisible={isHovered}
        />
      )}
    </div>
  );
};

const RealPortfolioCard: React.FC<Props> = ({ portfolio, onNewUpload, onRefresh }) => {
  const [isAnalyzingAssets, setIsAnalyzingAssets] = useState(false);
  const [expandedSection, setExpandedSection] = useState<string | null>('stocks');
  const hasTriggeredAnalysis = useRef(false);

  // Verificar se algum ativo ja tem analise
  const hasAssetAnalyses = portfolio.positions.some(p => p.aiRecommendation);

  const handleAnalyzeAssets = async () => {
    setIsAnalyzingAssets(true);
    try {
      await realPortfolioService.analyzeIndividualAssets(portfolio.userId);
      onRefresh();
    } catch (err) {
      console.error('Erro ao analisar ativos:', err);
    } finally {
      setIsAnalyzingAssets(false);
    }
  };

  // Acionar analise automaticamente ao abrir a aba se nao houver analises
  useEffect(() => {
    if (!hasAssetAnalyses && !hasTriggeredAnalysis.current && !isAnalyzingAssets) {
      hasTriggeredAnalysis.current = true;
      handleAnalyzeAssets();
    }
  }, [hasAssetAnalyses]);

  // Agrupar posicoes por tipo
  const stocks = portfolio.positions.filter((p) => p.assetType === 'ACAO');
  const fiis = portfolio.positions.filter((p) => p.assetType === 'FII');
  const fixedIncome = portfolio.positions.filter((p) =>
    ['CDB', 'LCA', 'LCI', 'DEBENTURE', 'RENDA_FIXA'].includes(p.assetType)
  );
  const funds = portfolio.positions.filter((p) =>
    ['FUNDO', 'FIAGRO'].includes(p.assetType)
  );

  // Calcular percentuais para o grafico
  const total = portfolio.grandTotal || 1;
  const stocksPct = ((portfolio.totalStocks || 0) / total) * 100;
  const fiisPct = ((portfolio.totalFiis || 0) / total) * 100;
  const fixedIncomePct = ((portfolio.totalFixedIncome || 0) / total) * 100;
  const fundsPct = ((portfolio.totalFunds || 0) / total) * 100;

  const toggleSection = (section: string) => {
    setExpandedSection(expandedSection === section ? null : section);
  };

  const renderPositionsTable = (positions: RealPortfolioPosition[], title: string, color: string) => {
    if (positions.length === 0) return null;

    return (
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        <button
          className={`w-full px-4 py-3 flex justify-between items-center ${color} text-white font-medium`}
          onClick={() => toggleSection(title)}
        >
          <span>{title} ({positions.length})</span>
          <span>{expandedSection === title ? '−' : '+'}</span>
        </button>
        {expandedSection === title && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-2 text-left">Ticker</th>
                  <th className="px-4 py-2 text-left">Nome</th>
                  <th className="px-4 py-2 text-right">Qtd</th>
                  <th className="px-4 py-2 text-right">Preco</th>
                  <th className="px-4 py-2 text-right">Preco Teto</th>
                  <th className="px-4 py-2 text-right">Total</th>
                  {title === 'Renda Fixa' && <th className="px-4 py-2 text-center">Vencimento</th>}
                  <th className="px-4 py-2 text-center">Vies</th>
                  <th className="px-4 py-2 text-center">Acao</th>
                </tr>
              </thead>
              <tbody>
                {positions.map((pos, idx) => (
                  <tr key={pos.id || idx} className={`border-t border-gray-100 hover:bg-gray-50 ${getRecommendationBgColor(pos.aiRecommendation)}`}>
                    <td className="px-4 py-2 font-medium text-gray-900">
                      {pos.ticker}
                      {pos.assetSubtype && (
                        <span className="ml-1 text-xs text-gray-500">({pos.assetSubtype})</span>
                      )}
                    </td>
                    <td className="px-4 py-2 text-gray-600 truncate max-w-[200px]">{pos.name}</td>
                    <td className="px-4 py-2 text-right text-gray-700">{formatNumber(pos.quantity)}</td>
                    <td className="px-4 py-2 text-right text-gray-700">{formatCurrency(pos.closePrice)}</td>
                    <td className="px-4 py-2 text-right text-gray-700">
                      {pos.aiCeilingPrice ? formatCurrency(pos.aiCeilingPrice) : '-'}
                    </td>
                    <td className="px-4 py-2 text-right font-medium text-gray-900">{formatCurrency(pos.totalValue)}</td>
                    {title === 'Renda Fixa' && (
                      <td className="px-4 py-2 text-center text-gray-600">{formatDate(pos.maturityDate)}</td>
                    )}
                    <BiasCell position={pos} />
                    <ActionCell position={pos} />
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  };

  const renderDividendsTable = (dividends: RealPortfolioDividend[]) => {
    if (dividends.length === 0) return null;

    return (
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        <button
          className="w-full px-4 py-3 flex justify-between items-center bg-green-600 text-white font-medium"
          onClick={() => toggleSection('dividends')}
        >
          <span>Proventos Recebidos ({dividends.length})</span>
          <span>{expandedSection === 'dividends' ? '−' : '+'}</span>
        </button>
        {expandedSection === 'dividends' && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-2 text-left">Ticker</th>
                  <th className="px-4 py-2 text-left">Produto</th>
                  <th className="px-4 py-2 text-center">Data</th>
                  <th className="px-4 py-2 text-center">Tipo</th>
                  <th className="px-4 py-2 text-right">Qtd</th>
                  <th className="px-4 py-2 text-right">Valor Unit.</th>
                  <th className="px-4 py-2 text-right">Valor Liq.</th>
                </tr>
              </thead>
              <tbody>
                {dividends.map((div, idx) => (
                  <tr key={div.id || idx} className="border-t border-gray-100 hover:bg-gray-50">
                    <td className="px-4 py-2 font-medium text-gray-900">{div.ticker}</td>
                    <td className="px-4 py-2 text-gray-600 truncate max-w-[150px]">{div.productName}</td>
                    <td className="px-4 py-2 text-center text-gray-600">{formatDate(div.paymentDate)}</td>
                    <td className="px-4 py-2 text-center">
                      <span className={`px-2 py-1 rounded-full text-xs ${
                        div.eventType?.includes('Dividendo') ? 'bg-blue-100 text-blue-700' :
                        div.eventType?.includes('JCP') || div.eventType?.includes('Juros') ? 'bg-purple-100 text-purple-700' :
                        'bg-green-100 text-green-700'
                      }`}>
                        {div.eventType?.includes('Juros') ? 'JCP' :
                         div.eventType?.includes('Dividendo') ? 'Dividendo' :
                         div.eventType?.includes('Rendimento') ? 'Rendimento' : div.eventType}
                      </span>
                    </td>
                    <td className="px-4 py-2 text-right text-gray-700">{formatNumber(div.quantity)}</td>
                    <td className="px-4 py-2 text-right text-gray-700">{formatCurrency(div.unitPrice)}</td>
                    <td className="px-4 py-2 text-right font-medium text-green-600">{formatCurrency(div.netValue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="space-y-6">
      {/* Header com totais */}
      <div className="bg-gradient-to-r from-purple-600 to-indigo-600 rounded-xl p-6 text-white">
        <div className="flex justify-between items-start">
          <div>
            <h2 className="text-2xl font-bold">Minha Carteira Real</h2>
            <p className="text-purple-200 mt-1">
              Ref: {portfolio.reportMonth?.toString().padStart(2, '0')}/{portfolio.reportYear}
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={handleAnalyzeAssets}
              disabled={isAnalyzingAssets}
              className="px-4 py-2 bg-white/20 hover:bg-white/30 rounded-lg text-sm font-medium transition-colors disabled:opacity-50"
              title="Analisar cada ativo individualmente com recomendacao de compra/venda"
            >
              {isAnalyzingAssets ? 'Analisando Ativos...' : hasAssetAnalyses ? 'Reanalisar Ativos' : 'Analisar Ativos'}
            </button>
            <button
              onClick={onNewUpload}
              className="px-4 py-2 bg-white/20 hover:bg-white/30 rounded-lg text-sm font-medium transition-colors"
            >
              Novo Upload
            </button>
          </div>
        </div>
        <div className="text-4xl font-bold mt-4">
          {formatCurrency(portfolio.grandTotal)}
        </div>
        <p className="text-purple-200 text-sm mt-1">
          Patrimonio total em investimentos
        </p>
      </div>

      {/* Health Score Card */}
      <HealthScoreCard
        healthScore={portfolio.healthScore}
        healthScoreDetails={portfolio.healthScoreDetails}
      />

      {/* Cards de resumo */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-blue-50 rounded-lg p-4 border border-blue-200">
          <div className="text-blue-600 text-sm font-medium">Acoes</div>
          <div className="text-xl font-bold text-blue-800">{formatCurrency(portfolio.totalStocks)}</div>
          <div className="text-blue-500 text-sm">{stocksPct.toFixed(1)}%</div>
        </div>
        <div className="bg-purple-50 rounded-lg p-4 border border-purple-200">
          <div className="text-purple-600 text-sm font-medium">FIIs</div>
          <div className="text-xl font-bold text-purple-800">{formatCurrency(portfolio.totalFiis)}</div>
          <div className="text-purple-500 text-sm">{fiisPct.toFixed(1)}%</div>
        </div>
        <div className="bg-amber-50 rounded-lg p-4 border border-amber-200">
          <div className="text-amber-600 text-sm font-medium">Renda Fixa</div>
          <div className="text-xl font-bold text-amber-800">{formatCurrency(portfolio.totalFixedIncome)}</div>
          <div className="text-amber-500 text-sm">{fixedIncomePct.toFixed(1)}%</div>
        </div>
        <div className="bg-green-50 rounded-lg p-4 border border-green-200">
          <div className="text-green-600 text-sm font-medium">Proventos</div>
          <div className="text-xl font-bold text-green-800">{formatCurrency(portfolio.totalDividends)}</div>
          <div className="text-green-500 text-sm">no mes</div>
        </div>
      </div>

      {/* Barra de alocacao */}
      <div className="bg-white rounded-lg p-4 border border-gray-200">
        <h3 className="text-sm font-medium text-gray-700 mb-3">Alocacao da Carteira</h3>
        <div className="h-6 rounded-full overflow-hidden flex">
          <div className="bg-blue-500" style={{ width: `${stocksPct}%` }} title={`Acoes: ${stocksPct.toFixed(1)}%`}></div>
          <div className="bg-purple-500" style={{ width: `${fiisPct}%` }} title={`FIIs: ${fiisPct.toFixed(1)}%`}></div>
          <div className="bg-amber-500" style={{ width: `${fixedIncomePct}%` }} title={`Renda Fixa: ${fixedIncomePct.toFixed(1)}%`}></div>
          <div className="bg-teal-500" style={{ width: `${fundsPct}%` }} title={`Fundos: ${fundsPct.toFixed(1)}%`}></div>
        </div>
        <div className="flex flex-wrap gap-4 mt-3 text-xs">
          <div className="flex items-center gap-1">
            <div className="w-3 h-3 rounded bg-blue-500"></div>
            <span>Acoes ({stocksPct.toFixed(1)}%)</span>
          </div>
          <div className="flex items-center gap-1">
            <div className="w-3 h-3 rounded bg-purple-500"></div>
            <span>FIIs ({fiisPct.toFixed(1)}%)</span>
          </div>
          <div className="flex items-center gap-1">
            <div className="w-3 h-3 rounded bg-amber-500"></div>
            <span>Renda Fixa ({fixedIncomePct.toFixed(1)}%)</span>
          </div>
          <div className="flex items-center gap-1">
            <div className="w-3 h-3 rounded bg-teal-500"></div>
            <span>Fundos ({fundsPct.toFixed(1)}%)</span>
          </div>
        </div>
      </div>

      {/* Tabelas de posicoes */}
      <div className="space-y-4">
        {renderPositionsTable(stocks, 'Acoes', 'bg-blue-600')}
        {renderPositionsTable(fiis, 'FIIs', 'bg-purple-600')}
        {renderPositionsTable(fixedIncome, 'Renda Fixa', 'bg-amber-600')}
        {renderPositionsTable(funds, 'Fundos', 'bg-teal-600')}
        {renderDividendsTable(portfolio.dividends)}
      </div>

      {/* Rodape com info */}
      <div className="text-center text-xs text-gray-500">
        <p>Dados importados do relatorio consolidado da B3</p>
        <p>Upload em: {portfolio.uploadedAt ? new Date(portfolio.uploadedAt).toLocaleString('pt-BR') : '-'}</p>
      </div>
    </div>
  );
};

export default RealPortfolioCard;
