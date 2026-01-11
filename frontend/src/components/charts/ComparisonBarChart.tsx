import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { MonthlySpending } from '../../types';

interface ComparisonBarChartProps {
  currentMonth: MonthlySpending;
  previousMonth?: MonthlySpending;
}

const ComparisonBarChart: React.FC<ComparisonBarChartProps> = ({ currentMonth, previousMonth }) => {
  if (!previousMonth) {
    return (
      <div className="flex items-center justify-center h-64 text-gray-500">
        Dados do mês anterior não disponíveis para comparação
      </div>
    );
  }

  // Mesclar categorias de ambos os meses
  const allCategories = new Set([
    ...currentMonth.categories.map(c => c.name),
    ...previousMonth.categories.map(c => c.name),
  ]);

  // Criar dados para o gráfico
  const chartData = Array.from(allCategories).map(categoryName => {
    const currentAmount = currentMonth.categories.find(c => c.name === categoryName)?.amount || 0;
    const previousAmount = previousMonth.categories.find(c => c.name === categoryName)?.amount || 0;

    return {
      category: categoryName,
      current: currentAmount,
      previous: previousAmount,
      increased: currentAmount > previousAmount,
    };
  })
  .sort((a, b) => b.current - a.current) // Ordenar por gasto atual
  .slice(0, 8); // Top 8 categorias

  // Formatar valor monetário
  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  // Custom Tooltip
  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const current = payload[0].value;
      const previous = payload[1].value;
      const diff = current - previous;
      const diffPercent = previous > 0 ? ((diff / previous) * 100).toFixed(1) : 'N/A';

      return (
        <div className="bg-white border-2 border-gray-200 rounded-lg p-3 shadow-lg">
          <p className="text-sm font-semibold text-gray-700 mb-2">{payload[0].payload.category}</p>
          <div className="space-y-1">
            <div className="flex items-center justify-between gap-3">
              <span className="text-xs text-gray-500">Mês Atual:</span>
              <span className="text-sm font-bold text-purple-600">{formatCurrency(current)}</span>
            </div>
            <div className="flex items-center justify-between gap-3">
              <span className="text-xs text-gray-500">Mês Anterior:</span>
              <span className="text-sm font-bold text-gray-600">{formatCurrency(previous)}</span>
            </div>
            {diffPercent !== 'N/A' && (
              <div className="flex items-center justify-between gap-3 pt-1 border-t border-gray-200">
                <span className="text-xs text-gray-500">Variação:</span>
                <span className={`text-sm font-bold ${diff > 0 ? 'text-red-600' : 'text-green-600'}`}>
                  {diff > 0 ? '+' : ''}{diffPercent}%
                </span>
              </div>
            )}
          </div>
        </div>
      );
    }
    return null;
  };

  return (
    <ResponsiveContainer width="100%" height={350}>
      <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 70 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
        <XAxis
          dataKey="category"
          angle={-45}
          textAnchor="end"
          height={100}
          tick={{ fill: '#374151', fontSize: 11 }}
          interval={0}
        />
        <YAxis
          tick={{ fill: '#6b7280', fontSize: 12 }}
          axisLine={{ stroke: '#d1d5db' }}
          tickFormatter={(value) => `R$ ${(value / 1000).toFixed(0)}k`}
        />
        <Tooltip content={<CustomTooltip />} />
        <Legend
          wrapperStyle={{ paddingTop: '10px' }}
          iconType="rect"
        />
        <Bar dataKey="current" fill="#8b5cf6" name="Mês Atual" radius={[8, 8, 0, 0]} />
        <Bar dataKey="previous" fill="#d1d5db" name="Mês Anterior" radius={[8, 8, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
};

export default ComparisonBarChart;
