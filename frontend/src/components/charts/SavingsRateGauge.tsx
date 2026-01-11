import React from 'react';
import { RadialBarChart, RadialBar, ResponsiveContainer, PolarAngleAxis } from 'recharts';

interface SavingsRateGaugeProps {
  rate: number; // 0-100 (percentage)
}

const SavingsRateGauge: React.FC<SavingsRateGaugeProps> = ({ rate }) => {
  // Clamp rate between 0 and 100
  const clampedRate = Math.max(0, Math.min(100, rate));

  // Determinar cor baseada no rate
  const getColor = (value: number): string => {
    if (value >= 20) return '#10b981'; // green-500
    if (value >= 10) return '#f59e0b'; // amber-500
    return '#ef4444'; // red-500
  };

  // Determinar status textual
  const getStatus = (value: number): string => {
    if (value >= 20) return 'SAUDÁVEL';
    if (value >= 10) return 'ATENÇÃO';
    return 'CRÍTICO';
  };

  const data = [
    {
      name: 'savings',
      value: clampedRate,
      fill: getColor(clampedRate),
    },
  ];

  return (
    <div className="relative flex flex-col items-center justify-center">
      <ResponsiveContainer width="100%" height={250}>
        <RadialBarChart
          cx="50%"
          cy="70%"
          innerRadius="60%"
          outerRadius="100%"
          barSize={20}
          data={data}
          startAngle={180}
          endAngle={0}
        >
          <PolarAngleAxis type="number" domain={[0, 100]} angleAxisId={0} tick={false} />
          <RadialBar
            background
            dataKey="value"
            cornerRadius={10}
            fill={getColor(clampedRate)}
          />
        </RadialBarChart>
      </ResponsiveContainer>

      {/* Número central sobreposto */}
      <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/4 text-center">
        <div className="text-5xl font-bold" style={{ color: getColor(clampedRate) }}>
          {clampedRate.toFixed(1)}%
        </div>
        <div className="text-sm text-gray-600 font-semibold mt-1">
          {getStatus(clampedRate)}
        </div>
      </div>

      {/* Legenda de escala */}
      <div className="flex justify-between items-center w-full max-w-xs mt-2 text-xs text-gray-500">
        <span className="flex items-center gap-1">
          <div className="w-3 h-3 rounded-full bg-red-500"></div>
          0-9%
        </span>
        <span className="flex items-center gap-1">
          <div className="w-3 h-3 rounded-full bg-amber-500"></div>
          10-19%
        </span>
        <span className="flex items-center gap-1">
          <div className="w-3 h-3 rounded-full bg-green-500"></div>
          20%+
        </span>
      </div>
    </div>
  );
};

export default SavingsRateGauge;
