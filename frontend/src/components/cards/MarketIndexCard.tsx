import React from 'react';

interface MarketIndexCardProps {
  symbol: string;
  name: string;
  value: number;
  change: number;
  trend: 'up' | 'down' | 'neutral';
  format?: 'currency' | 'number';
}

const MarketIndexCard: React.FC<MarketIndexCardProps> = ({
  symbol,
  name,
  value,
  change,
  trend,
  format = 'number',
}) => {
  const trendIcon = trend === 'up' ? '↑' : trend === 'down' ? '↓' : '→';
  const trendColor =
    trend === 'up'
      ? 'text-green-600'
      : trend === 'down'
      ? 'text-red-600'
      : 'text-gray-600';

  const formatValue = (val: number) => {
    if (format === 'currency') {
      return new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }).format(val);
    }
    return new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(val);
  };

  return (
    <div className="bg-white rounded-lg p-4 border border-gray-200 shadow-sm hover:shadow-md transition-shadow">
      <div className="text-xs text-gray-500 mb-1">{symbol}</div>
      <div className="text-sm font-semibold text-gray-700 mb-2">{name}</div>
      <div className="text-2xl font-bold text-gray-900 mb-1">
        {formatValue(value)}
      </div>
      <div className={`text-sm font-semibold flex items-center gap-1 ${trendColor}`}>
        <span>{trendIcon}</span>
        <span>
          {change >= 0 ? '+' : ''}
          {change.toFixed(2)}%
        </span>
      </div>
    </div>
  );
};

export default MarketIndexCard;
