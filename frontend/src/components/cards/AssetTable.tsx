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
      const tooltipWidth = 350;
      const viewportWidth = window.innerWidth;

      // Posicionar mais à esquerda do ícone
      let left = rect.left - tooltipWidth - 10;

      // Se ultrapassar a borda esquerda, colocar à direita do ícone
      if (left < 10) {
        left = rect.right + 10;
      }

      // Se ainda ultrapassar a borda direita, centralizar na tela
      if (left + tooltipWidth > viewportWidth - 10) {
        left = Math.max(10, (viewportWidth - tooltipWidth) / 2);
      }

      setCoords({
        top: rect.bottom + 8,
        left: left,
      });
    }
  }, [anchorRef, isVisible]);

  if (!isVisible) return null;

  // Helpers para formatação
  const formatCurrency = (value: number | null | undefined) => {
    if (value === null || value === undefined) return '-';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  const formatPercent = (value: number | null | undefined) => {
    if (value === null || value === undefined) return '-';
    return `${value.toFixed(1)}%`;
  };

  // Helpers para status de indicadores
  const getPriceStatus = () => {
    if (!asset.currentPrice || !asset.ceilingPrice) return null;
    const ratio = asset.currentPrice / asset.ceilingPrice;
    if (ratio <= 0.8) return { label: 'Muito Barato', color: 'text-green-600', bg: 'bg-green-100' };
    if (ratio <= 1.0) return { label: 'Bom Preço', color: 'text-green-600', bg: 'bg-green-100' };
    if (ratio <= 1.1) return { label: 'No Limite', color: 'text-yellow-600', bg: 'bg-yellow-100' };
    return { label: 'Acima do Teto', color: 'text-red-600', bg: 'bg-red-100' };
  };

  const getDYStatus = (dy: number | null | undefined) => {
    if (!dy) return null;
    if (dy >= 8) return { label: 'Excelente', color: 'text-green-600' };
    if (dy >= 6) return { label: 'Bom', color: 'text-green-600' };
    if (dy >= 4) return { label: 'Razoável', color: 'text-yellow-600' };
    return { label: 'Baixo', color: 'text-gray-500' };
  };

  const priceStatus = getPriceStatus();
  const dyStatus = getDYStatus(asset.expectedDY);

  return ReactDOM.createPortal(
    <div
      className="fixed z-[9999] bg-white border border-gray-200 rounded-xl shadow-2xl p-4 w-[350px]"
      style={{ top: coords.top, left: coords.left }}
    >
      {/* Header com ticker */}
      <div className="flex items-center justify-between mb-3 pb-2 border-b border-gray-100">
        <div className="flex items-center gap-2">
          <span className="text-lg">📊</span>
          <span className="font-bold text-gray-800 text-lg">{asset.ticker}</span>
        </div>
        <span className="text-xs px-2 py-1 bg-indigo-100 text-indigo-700 rounded-full">{asset.type}</span>
      </div>

      {/* Grid de Indicadores */}
      <div className="grid grid-cols-2 gap-2 mb-3">
        {/* Preço Atual */}
        <div className="bg-gray-50 rounded-lg p-2">
          <span className="text-xs text-gray-500 block">Preço Atual</span>
          <span className="font-bold text-gray-900">{formatCurrency(asset.currentPrice)}</span>
        </div>

        {/* Preço Teto */}
        <div className="bg-gray-50 rounded-lg p-2">
          <span className="text-xs text-gray-500 block">Preço Teto</span>
          <span className="font-bold text-gray-900">{formatCurrency(asset.ceilingPrice)}</span>
        </div>

        {/* DY Esperado */}
        {asset.expectedDY !== null && asset.expectedDY !== undefined && (
          <div className="bg-gray-50 rounded-lg p-2">
            <span className="text-xs text-gray-500 block">Dividend Yield</span>
            <div className="flex items-center gap-1">
              <span className="font-bold text-gray-900">{formatPercent(asset.expectedDY)}</span>
              {dyStatus && (
                <span className={`text-xs ${dyStatus.color}`}>({dyStatus.label})</span>
              )}
            </div>
          </div>
        )}

        {/* Alocação */}
        <div className="bg-gray-50 rounded-lg p-2">
          <span className="text-xs text-gray-500 block">Alocação Sugerida</span>
          <span className="font-bold text-indigo-600">{asset.targetAllocation}%</span>
        </div>
      </div>

      {/* Status do Preço */}
      {priceStatus && (
        <div className={`rounded-lg p-2 mb-3 ${priceStatus.bg}`}>
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-600">Status do Preço:</span>
            <span className={`font-bold text-sm ${priceStatus.color}`}>{priceStatus.label}</span>
          </div>
          {asset.currentPrice && asset.ceilingPrice && (
            <div className="mt-1">
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className={`h-2 rounded-full ${
                    asset.currentPrice <= asset.ceilingPrice ? 'bg-green-500' : 'bg-red-500'
                  }`}
                  style={{ width: `${Math.min(100, (asset.currentPrice / asset.ceilingPrice) * 100)}%` }}
                />
              </div>
              <div className="flex justify-between text-xs text-gray-500 mt-1">
                <span>0%</span>
                <span>Teto (100%)</span>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Análise/Rationale */}
      {asset.rationale && (
        <div className="bg-indigo-50 rounded-lg p-3">
          <div className="flex items-center gap-1 mb-1">
            <span className="text-sm">💡</span>
            <span className="text-xs font-semibold text-indigo-800">Por que escolhemos?</span>
          </div>
          <p className="text-sm text-gray-700 leading-relaxed">
            {asset.rationale}
          </p>
        </div>
      )}

      {/* Viés */}
      {asset.bias && asset.bias !== '-' && (
        <div className="mt-3 flex items-center justify-center">
          <span
            className={`px-4 py-2 rounded-full text-sm font-bold ${
              asset.bias === 'Comprar'
                ? 'bg-green-100 text-green-800'
                : 'bg-yellow-100 text-yellow-800'
            }`}
          >
            {asset.bias === 'Comprar' ? '✅ Momento de Compra' : '⏳ Aguardar Melhor Preço'}
          </span>
        </div>
      )}

      {/* Seta indicadora */}
      <div className="absolute -top-2 left-1/2 -translate-x-1/2 w-4 h-4 bg-white border-l border-t border-gray-200 rotate-45" />
    </div>,
    document.body
  );
};

// Celula de Viés com tooltip
interface BiasCellProps {
  asset: RecommendedAsset;
}

const getBiasStyle = (bias: string | undefined) => {
  switch (bias) {
    case 'Comprar':
      return 'bg-green-100 text-green-800 hover:bg-green-200';
    case 'Aguardar':
      return 'bg-yellow-100 text-yellow-800 hover:bg-yellow-200';
    default:
      return 'bg-gray-100 text-gray-600 hover:bg-gray-200';
  }
};

const BiasCell: React.FC<BiasCellProps> = ({ asset }) => {
  const [isHovered, setIsHovered] = useState(false);
  const anchorRef = useRef<HTMLSpanElement>(null);

  return (
    <td className="px-3 py-3 text-center" onClick={(e) => e.stopPropagation()}>
      <span
        ref={anchorRef}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
        className={`px-2 py-1 rounded-full text-xs font-semibold cursor-help transition-colors ${getBiasStyle(asset.bias)}`}
      >
        {asset.bias || '-'}
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
                Info
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
                <BiasCell asset={asset} />
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
              <BiasCell asset={asset} />
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default AssetTable;
