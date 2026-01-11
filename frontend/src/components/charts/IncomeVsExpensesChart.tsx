import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, Cell } from 'recharts';
import { HouseholdIncomeAnalysis } from '../../types';

interface IncomeVsExpensesChartProps {
  data: HouseholdIncomeAnalysis;
}

const IncomeVsExpensesChart: React.FC<IncomeVsExpensesChartProps> = ({ data }) => {
  // Formatar valor monetário
  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  // Dados para o gráfico de barras
  const chartData = [
    {
      category: 'Mariana',
      value: data.marianaIncome,
      type: 'income',
      label: 'Renda Fixa',
    },
    {
      category: 'Lucas',
      value: data.lucasNetIncome,
      type: 'income',
      label: 'Renda Variável (USD)',
    },
    {
      category: 'Gastos',
      value: data.totalExpenses,
      type: 'expense',
      label: 'Gastos Totais',
    },
  ];

  // Cores
  const getColor = (type: string) => {
    if (type === 'income') return '#10b981'; // green-500
    return '#ef4444'; // red-500
  };

  // Custom Tooltip
  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const item = payload[0].payload;
      return (
        <div className="bg-white border-2 border-gray-200 rounded-lg p-3 shadow-lg">
          <p className="text-sm font-semibold text-gray-700 mb-1">{item.category}</p>
          <p className="text-xs text-gray-500 mb-1">{item.label}</p>
          <p className={`text-lg font-bold ${item.type === 'income' ? 'text-green-600' : 'text-red-600'}`}>
            {formatCurrency(item.value)}
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div>
      <ResponsiveContainer width="100%" height={250}>
        <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis
            dataKey="category"
            tick={{ fill: '#374151', fontSize: 12, fontWeight: 500 }}
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
            formatter={(value, entry: any) => {
              if (entry.payload && entry.payload.type === 'income') return 'Renda';
              return 'Gastos';
            }}
          />
          <Bar dataKey="value" radius={[8, 8, 0, 0]}>
            {chartData.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={getColor(entry.type)} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>

      {/* Insight textual */}
      <div className={`mt-3 px-4 py-3 rounded-r-lg border-l-4 ${
        data.savings >= 0 ? 'bg-green-50 border-green-500' : 'bg-red-50 border-red-500'
      }`}>
        <p className="text-sm text-gray-700">
          <span className="font-semibold text-gray-800">💡 Análise: </span>
          {data.savings >= 0 ? (
            <>
              A casa está <span className="font-bold text-green-600">economizando</span>{' '}
              {formatCurrency(data.savings)} por mês ({data.savingsRate.toFixed(1)}% da renda).
            </>
          ) : (
            <>
              <span className="font-bold text-red-600">ALERTA:</span> Os gastos excedem a renda em{' '}
              {formatCurrency(Math.abs(data.savings))}. Revisem o orçamento urgentemente!
            </>
          )}
        </p>
      </div>
    </div>
  );
};

export default IncomeVsExpensesChart;
