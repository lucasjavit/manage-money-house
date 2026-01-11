import React from 'react';
import { RadialBarChart, RadialBar, ResponsiveContainer, PolarAngleAxis } from 'recharts';

interface HealthScoreGaugeProps {
  score: number; // 0-100
}

const HealthScoreGauge: React.FC<HealthScoreGaugeProps> = ({ score }) => {
  // Determinar cor baseada no score
  const getColor = (value: number): string => {
    if (value >= 70) return '#10b981'; // green-500
    if (value >= 40) return '#f59e0b'; // amber-500
    return '#ef4444'; // red-500
  };

  // Determinar status textual
  const getStatus = (value: number): string => {
    if (value >= 70) return 'BOA';
    if (value >= 40) return 'ATENÇÃO';
    return 'CRÍTICA';
  };

  const data = [
    {
      name: 'score',
      value: score,
      fill: getColor(score),
    },
  ];

  return (
    <div className="relative flex flex-col items-center justify-center">
      <ResponsiveContainer width="100%" height={200}>
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
            fill={getColor(score)}
          />
        </RadialBarChart>
      </ResponsiveContainer>

      {/* Número central sobreposto */}
      <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/4 text-center">
        <div className="text-5xl font-bold" style={{ color: getColor(score) }}>
          {score}
        </div>
        <div className="text-sm text-gray-600 font-semibold mt-1">
          {getStatus(score)}
        </div>
      </div>

      {/* Legenda de escala */}
      <div className="flex justify-between items-center w-full max-w-xs mt-2 text-xs text-gray-500">
        <span className="flex items-center gap-1">
          <div className="w-3 h-3 rounded-full bg-red-500"></div>
          0-39
        </span>
        <span className="flex items-center gap-1">
          <div className="w-3 h-3 rounded-full bg-amber-500"></div>
          40-69
        </span>
        <span className="flex items-center gap-1">
          <div className="w-3 h-3 rounded-full bg-green-500"></div>
          70-100
        </span>
      </div>
    </div>
  );
};

export default HealthScoreGauge;
