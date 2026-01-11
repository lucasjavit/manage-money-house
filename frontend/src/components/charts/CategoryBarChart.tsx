import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { CategoryAmount } from '../../types';

interface CategoryBarChartProps {
  categories: CategoryAmount[];
}

const CategoryBarChart: React.FC<CategoryBarChartProps> = ({ categories }) => {
  // Ordenar por valor decrescente e pegar top 10
  const sortedData = [...categories]
    .sort((a, b) => b.amount - a.amount)
    .slice(0, 10);

  // Calcular total para percentuais
  const total = categories.reduce((sum, cat) => sum + cat.amount, 0);

  // Transformar dados para o formato do Recharts
  const chartData = sortedData.map(item => ({
    name: item.name,
    amount: item.amount,
    percentage: ((item.amount / total) * 100).toFixed(1),
  }));

  // Determinar cor baseada na posição (quanto maior o gasto, mais vermelho)
  const getColor = (index: number): string => {
    const colors = [
      '#ef4444', // red-500 - maior gasto
      '#f97316', // orange-500
      '#f59e0b', // amber-500
      '#eab308', // yellow-500
      '#84cc16', // lime-500
      '#22c55e', // green-500
      '#10b981', // emerald-500
      '#14b8a6', // teal-500
      '#06b6d4', // cyan-500
      '#0ea5e9', // sky-500
    ];
    return colors[index % colors.length];
  };

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
      return (
        <div className="bg-white border-2 border-gray-200 rounded-lg p-3 shadow-lg">
          <p className="text-sm font-semibold text-gray-700">{payload[0].payload.name}</p>
          <p className="text-lg font-bold text-purple-600">
            {formatCurrency(payload[0].value)}
          </p>
          <p className="text-xs text-gray-500">
            {payload[0].payload.percentage}% do total
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <ResponsiveContainer width="100%" height={400}>
      <BarChart
        data={chartData}
        layout="vertical"
        margin={{ top: 5, right: 30, left: 100, bottom: 5 }}
      >
        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" horizontal={false} />
        <XAxis
          type="number"
          tick={{ fill: '#6b7280', fontSize: 12 }}
          axisLine={{ stroke: '#d1d5db' }}
          tickFormatter={(value) => `R$ ${(value / 1000).toFixed(0)}k`}
        />
        <YAxis
          type="category"
          dataKey="name"
          tick={{ fill: '#374151', fontSize: 13, fontWeight: 500 }}
          axisLine={{ stroke: '#d1d5db' }}
          width={90}
        />
        <Tooltip content={<CustomTooltip />} />
        <Bar dataKey="amount" radius={[0, 8, 8, 0]}>
          {chartData.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={getColor(index)} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
};

export default CategoryBarChart;
