import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { MonthlyIncomeData } from '../../types';

interface IncomeStabilityChartProps {
  historicalData: MonthlyIncomeData[];
}

const IncomeStabilityChart: React.FC<IncomeStabilityChartProps> = ({ historicalData }) => {
  // Formatar mês de "2025-08" para "Ago/25"
  function formatMonth(monthStr: string): string {
    const [year, month] = monthStr.split('-');
    const monthNames = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
    return `${monthNames[parseInt(month) - 1]}/${year.slice(2)}`;
  }

  // Formatar valor monetário
  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  // Transformar dados para o formato do Recharts
  const chartData = historicalData.map(item => ({
    month: formatMonth(item.month),
    mariana: item.marianaIncome,
    lucas: item.lucasIncome,
    total: item.totalIncome,
  }));

  // Custom Tooltip
  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-white border-2 border-gray-200 rounded-lg p-3 shadow-lg">
          <p className="text-sm font-semibold text-gray-700 mb-2">{payload[0].payload.month}</p>
          <div className="space-y-1">
            <div className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-emerald-500"></div>
                <span className="text-xs text-gray-600">Mariana (fixa):</span>
              </div>
              <span className="text-sm font-bold text-emerald-600">
                {formatCurrency(payload[0].value)}
              </span>
            </div>
            <div className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-purple-600"></div>
                <span className="text-xs text-gray-600">Lucas (USD):</span>
              </div>
              <span className="text-sm font-bold text-purple-600">
                {formatCurrency(payload[1].value)}
              </span>
            </div>
            <div className="flex items-center justify-between gap-4 pt-1 border-t border-gray-200">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-blue-600"></div>
                <span className="text-xs text-gray-600">Total Casa:</span>
              </div>
              <span className="text-sm font-bold text-blue-600">
                {formatCurrency(payload[2].value)}
              </span>
            </div>
          </div>
        </div>
      );
    }
    return null;
  };

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
        <XAxis
          dataKey="month"
          tick={{ fill: '#6b7280', fontSize: 12 }}
          axisLine={{ stroke: '#d1d5db' }}
        />
        <YAxis
          tick={{ fill: '#6b7280', fontSize: 12 }}
          axisLine={{ stroke: '#d1d5db' }}
          tickFormatter={(value) => `R$ ${(value / 1000).toFixed(0)}k`}
        />
        <Tooltip content={<CustomTooltip />} />
        <Legend
          wrapperStyle={{ paddingTop: '10px' }}
          iconType="line"
        />
        <Line
          type="monotone"
          dataKey="mariana"
          stroke="#10b981"
          strokeWidth={2}
          dot={{ fill: '#10b981', r: 4 }}
          name="Mariana (Fixa)"
        />
        <Line
          type="monotone"
          dataKey="lucas"
          stroke="#8b5cf6"
          strokeWidth={3}
          dot={{ fill: '#8b5cf6', r: 5 }}
          activeDot={{ r: 7 }}
          name="Lucas (Variável USD)"
        />
        <Line
          type="monotone"
          dataKey="total"
          stroke="#3b82f6"
          strokeWidth={2}
          strokeDasharray="5 5"
          dot={{ fill: '#3b82f6', r: 3 }}
          name="Total Casa"
        />
      </LineChart>
    </ResponsiveContainer>
  );
};

export default IncomeStabilityChart;
