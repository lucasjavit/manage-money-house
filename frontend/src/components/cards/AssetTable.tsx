import { useState, useRef, useEffect } from 'react';
import ReactDOM from 'react-dom';
import type { RecommendedAsset } from '../../types';

interface AssetTableProps {
  assets: RecommendedAsset[];
  showDY?: boolean;
  isFixedIncome?: boolean;
  onAssetSelect?: (asset: RecommendedAsset) => void;
}

// Tooltip Portal para mostrar análise do ativo
interface RationaleTooltipProps {
  asset: RecommendedAsset;
  anchorRef: React.RefObject<HTMLElement | null>;
  isVisible: boolean;
}

const RationaleTooltipPortal: React.FC<RationaleTooltipProps> = ({ asset, anchorRef, isVisible }) => {
  const [coords, setCoords] = useState({ top: 0, left: 0 });

  useEffect(() => {
    if (anchorRef.current && isVisible) {
      const rect = anchorRef.current.getBoundingClientRect();
      setCoords({
        top: rect.bottom + window.scrollY + 8,
        left: rect.left + window.scrollX + rect.width / 2 - 160,
      });
    }
  }, [anchorRef, isVisible]);

  if (!isVisible || !asset.rationale) return null;

  return ReactDOM.createPortal(
    <div
      className="fixed z-[9999] bg-white border border-gray-200 rounded-xl shadow-xl p-4 w-80 pointer-events-none"
      style={{ top: coords.top, left: Math.max(10, coords.left) }}
    >
      {/* Header com ticker */}
      <div className="flex items-center gap-2 mb-2 pb-2 border-b border-gray-100">
        <span className="text-lg">💡</span>
        <span className="font-bold text-gray-800">{asset.ticker}</span>
        <span className="text-xs text-gray-500">- Por que escolhemos?</span>
      </div>

      {/* Análise/Rationale */}
      <p className="text-sm text-gray-700 leading-relaxed">
        {asset.rationale}
      </p>

      {/* Info adicional se tiver DY */}
      {asset.expectedDY && (
        <div className="mt-3 pt-2 border-t border-gray-100 flex items-center gap-2">
          <span className="text-xs text-gray-500">DY Esperado:</span>
          <span className="text-xs font-semibold text-green-600">{asset.expectedDY.toFixed(1)}%</span>
        </div>
      )}

      {/* Seta indicadora */}
      <div className="absolute -top-2 left-1/2 -translate-x-1/2 w-4 h-4 bg-white border-l border-t border-gray-200 rotate-45" />
    </div>,
    document.body
  );
};

// Celula com tooltip de análise
interface AnalysisCellProps {
  asset: RecommendedAsset;
}

const AnalysisCell: React.FC<AnalysisCellProps> = ({ asset }) => {
  const [isHovered, setIsHovered] = useState(false);
  const anchorRef = useRef<HTMLSpanElement>(null);

  if (!asset.rationale) {
    return (
      <td className="px-3 py-3 text-center">
        <span className="text-gray-400 text-xs">-</span>
      </td>
    );
  }

  return (
    <td className="px-3 py-3 text-center">
      <span
        ref={anchorRef}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
        className="inline-flex items-center justify-center w-7 h-7 rounded-full bg-indigo-100 hover:bg-indigo-200 cursor-help transition-colors"
      >
        <svg className="w-4 h-4 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </span>
      <RationaleTooltipPortal asset={asset} anchorRef={anchorRef} isVisible={isHovered} />
    </td>
  );
};

const AssetTable = ({ assets, showDY = false, isFixedIncome = false, onAssetSelect }: AssetTableProps) => {
  const formatCurrency = (value: number | null | undefined) => {
    if (value === null || value === undefined) return '-';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  };

  const formatDY = (value: number | null | undefined) => {
    if (value === null || value === undefined) return '-';
    return `${value.toFixed(1)}%`;
  };

  const getBiasStyle = (bias: string | undefined) => {
    switch (bias) {
      case 'Comprar':
        return 'bg-green-100 text-green-800';
      case 'Aguardar':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-600';
    }
  };

  // Tabela simplificada para Renda Fixa
  if (isFixedIncome) {
    return (
      <div className="overflow-x-auto rounded-lg border border-gray-200">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                #
              </th>
              <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                Ticker / Produto
              </th>
              <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                Tipo
              </th>
              <th className="px-3 py-3 text-center text-xs font-semibold text-gray-600 uppercase tracking-wider">
                Alocacao
              </th>
              <th className="px-3 py-3 text-center text-xs font-semibold text-gray-600 uppercase tracking-wider">
                Analise
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {assets.map((asset) => (
              <tr
                key={asset.ticker}
                onClick={() => onAssetSelect?.(asset)}
                className="hover:bg-indigo-50 transition-colors cursor-pointer"
              >
                <td className="px-3 py-3 text-sm text-gray-500 font-medium">
                  {asset.rank}
                </td>
                <td className="px-3 py-3">
                  <div className="font-bold text-gray-900">{asset.ticker}</div>
                  <div className="text-xs text-gray-500">{asset.name}</div>
                </td>
                <td className="px-3 py-3 text-sm text-gray-600">
                  {asset.type}
                </td>
                <td className="px-3 py-3 text-center text-sm font-semibold text-indigo-600">
                  {asset.targetAllocation}%
                </td>
                <AnalysisCell asset={asset} />
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  // Tabela padrão para outras carteiras
  return (
    <div className="overflow-x-auto rounded-lg border border-gray-200">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
              #
            </th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
              Ticker / Empresa
            </th>
            <th className="px-3 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
              Tipo
            </th>
            {showDY && (
              <th className="px-3 py-3 text-center text-xs font-semibold text-gray-600 uppercase tracking-wider">
                DY
              </th>
            )}
            <th className="px-3 py-3 text-right text-xs font-semibold text-gray-600 uppercase tracking-wider">
              Atual
            </th>
            <th className="px-3 py-3 text-right text-xs font-semibold text-gray-600 uppercase tracking-wider">
              Teto
            </th>
            <th className="px-3 py-3 text-center text-xs font-semibold text-gray-600 uppercase tracking-wider">
              Alocacao
            </th>
            <th className="px-3 py-3 text-center text-xs font-semibold text-gray-600 uppercase tracking-wider">
              Vies
            </th>
            <th className="px-3 py-3 text-center text-xs font-semibold text-gray-600 uppercase tracking-wider">
              Analise
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 bg-white">
          {assets.map((asset) => (
            <tr
              key={asset.ticker}
              onClick={() => onAssetSelect?.(asset)}
              className="hover:bg-indigo-50 transition-colors cursor-pointer"
            >
              <td className="px-3 py-3 text-sm text-gray-500 font-medium">
                {asset.rank}
              </td>
              <td className="px-3 py-3">
                <div className="font-bold text-gray-900">{asset.ticker}</div>
                <div className="text-xs text-gray-500">{asset.name}</div>
              </td>
              <td className="px-3 py-3 text-sm text-gray-600">
                {asset.type}
              </td>
              {showDY && (
                <td className="px-3 py-3 text-center text-sm text-gray-700">
                  {formatDY(asset.expectedDY)}
                </td>
              )}
              <td className="px-3 py-3 text-right text-sm font-medium text-gray-900">
                {formatCurrency(asset.currentPrice)}
              </td>
              <td className="px-3 py-3 text-right text-sm text-gray-700">
                {formatCurrency(asset.ceilingPrice)}
              </td>
              <td className="px-3 py-3 text-center text-sm font-semibold text-indigo-600">
                {asset.targetAllocation}%
              </td>
              <td className="px-3 py-3 text-center">
                <span
                  className={`px-2 py-1 rounded-full text-xs font-semibold ${getBiasStyle(asset.bias)}`}
                >
                  {asset.bias || '-'}
                </span>
              </td>
              <AnalysisCell asset={asset} />
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default AssetTable;
