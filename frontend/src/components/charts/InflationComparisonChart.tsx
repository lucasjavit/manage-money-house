import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { MonthlySpending } from '../../types';

interface InflationComparisonChartProps {
  historicalData: MonthlySpending[];
  // Nota: IPCA e IGP-M viriam do economicContext em valores mensais históricos
  // Por simplicidade, vamos usar valores mock ou calculados
}

const InflationComparisonChart: React.FC<InflationComparisonChartProps> = ({ historicalData }) => {
  // Calcular variação percentual mês a mês dos gastos
  const chartData = historicalData.map((item, index) => {
    const month = formatMonth(item.month);
    const total = item.total;

    // Calcular variação percentual vs primeiro mês (base 100)
    const baseTotal = historicalData[0]?.total || 1;
    const spendingGrowth = ((total - baseTotal) / baseTotal) * 100;

    // Mock de inflação acumulada (em um cenário real, viria do backend)
    // Valores aproximados de IPCA e IGP-M para demonstração
    const ipcaGrowth = index * 0.5; // ~0.5% ao mês
    const igpmGrowth = index * 0.4; // ~0.4% ao mês

    return {
      month,
      spending: parseFloat(spendingGrowth.toFixed(2)),
      ipca: parseFloat(ipcaGrowth.toFixed(2)),
      igpm: parseFloat(igpmGrowth.toFixed(2)),
    };
  });

  // Formatar mês de "2025-08" para "Ago/25"
  function formatMonth(monthStr: string): string {
    const [year, month] = monthStr.split('-');
    const monthNames = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
    return `${monthNames[parseInt(month) - 1]}/${year.slice(2)}`;
  }

  // Custom Tooltip
  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-white border-2 border-gray-200 rounded-lg p-3 shadow-lg">
          <p className="text-sm font-semibold text-gray-700 mb-2">{payload[0].payload.month}</p>
          <div className="space-y-1">
            <div className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-purple-600"></div>
                <span className="text-xs text-gray-600">Seus Gastos:</span>
              </div>
              <span className="text-sm font-bold text-purple-600">
                {payload[0].value > 0 ? '+' : ''}{payload[0].value.toFixed(1)}%
              </span>
            </div>
            <div className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-red-500"></div>
                <span className="text-xs text-gray-600">IPCA:</span>
              </div>
              <span className="text-sm font-bold text-red-600">
                +{payload[1].value.toFixed(1)}%
              </span>
            </div>
            <div className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-orange-500"></div>
                <span className="text-xs text-gray-600">IGP-M:</span>
              </div>
              <span className="text-sm font-bold text-orange-600">
                +{payload[2].value.toFixed(1)}%
              </span>
            </div>
          </div>
          {payload[0].value > payload[1].value && (
            <p className="text-xs text-red-600 mt-2 pt-2 border-t border-gray-200 font-semibold">
              ⚠️ Gastos acima da inflação!
            </p>
          )}
        </div>
      );
    }
    return null;
  };

  return (
    <div>
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
            tickFormatter={(value) => `${value}%`}
          />
          <Tooltip content={<CustomTooltip />} />
          <Legend
            wrapperStyle={{ paddingTop: '10px' }}
            iconType="line"
          />
          <Line
            type="monotone"
            dataKey="spending"
            stroke="#8b5cf6"
            strokeWidth={3}
            dot={{ fill: '#8b5cf6', r: 4 }}
            name="Seus Gastos"
          />
          <Line
            type="monotone"
            dataKey="ipca"
            stroke="#ef4444"
            strokeWidth={2}
            strokeDasharray="5 5"
            dot={{ fill: '#ef4444', r: 3 }}
            name="IPCA"
          />
          <Line
            type="monotone"
            dataKey="igpm"
            stroke="#f97316"
            strokeWidth={2}
            strokeDasharray="5 5"
            dot={{ fill: '#f97316', r: 3 }}
            name="IGP-M"
          />
        </LineChart>
      </ResponsiveContainer>

      {/* Insight textual */}
      <div className="mt-3 px-4 py-3 bg-purple-50 border-l-4 border-purple-500 rounded-r-lg">
        <p className="text-sm text-gray-700">
          <span className="font-semibold text-purple-700">💡 Insight: </span>
          {chartData[chartData.length - 1].spending > chartData[chartData.length - 1].ipca ? (
            <>
              Seus gastos cresceram{' '}
              <span className="font-bold text-red-600">
                {(chartData[chartData.length - 1].spending - chartData[chartData.length - 1].ipca).toFixed(1)}%
              </span>
              {' '}acima da inflação (IPCA). Considere revisar seus hábitos de consumo.
            </>
          ) : (
            <>
              Seus gastos estão crescendo{' '}
              <span className="font-bold text-green-600">abaixo da inflação</span>. Parabéns pelo controle financeiro!
            </>
          )}
        </p>
      </div>
    </div>
  );
};

export default InflationComparisonChart;
