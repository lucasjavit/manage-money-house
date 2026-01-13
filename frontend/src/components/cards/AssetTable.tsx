import type { RecommendedAsset } from '../../types';

interface AssetTableProps {
  assets: RecommendedAsset[];
  showDY?: boolean;
  isFixedIncome?: boolean;
  onAssetSelect?: (asset: RecommendedAsset) => void;
}

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
              <td className="px-3 py-3 text-center">
                <span
                  className={`px-2 py-1 rounded-full text-xs font-semibold ${getBiasStyle(asset.bias)}`}
                >
                  {asset.bias || '-'}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default AssetTable;
