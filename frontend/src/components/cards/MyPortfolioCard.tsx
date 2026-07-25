import React, { useState } from 'react';
import type { InvestmentPortfolio, RecommendedAsset } from '../../types';
import AssetTable from './AssetTable';
import AssetDetailModal from './AssetDetailModal';

interface MyPortfolioCardProps {
  portfolio: InvestmentPortfolio;
  onRegenerate: () => void;
}

const MyPortfolioCard: React.FC<MyPortfolioCardProps> = ({ portfolio, onRegenerate }) => {
  const [selectedAsset, setSelectedAsset] = useState<RecommendedAsset | null>(null);
  const [investInput, setInvestInput] = useState('');

  const investAmount = parseFloat(investInput.replace(/\./g, '').replace(',', '.')) || 0;
  const fmtBRL = (v: number) =>
    new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);

  // Separar ativos em bom preco vs acima do teto
  const recommendedAssets = portfolio.recommendedAssets || [];

  const goodPriceAssets = recommendedAssets.filter((asset) => {
    if (asset.currentPrice == null || asset.ceilingPrice == null) return true;
    return asset.currentPrice <= asset.ceilingPrice;
  });

  const aboveCeilingAssets = recommendedAssets.filter((asset) => {
    if (asset.currentPrice == null || asset.ceilingPrice == null) return false;
    return asset.currentPrice > asset.ceilingPrice;
  });

  const getRiskBadgeColor = (riskLevel: string) => {
    switch (riskLevel.toLowerCase()) {
      case 'baixo':
        return 'bg-green-100 text-green-800';
      case 'moderado':
        return 'bg-blue-100 text-blue-800';
      case 'alto':
        return 'bg-orange-100 text-orange-800';
      case 'muito alto':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="space-y-6">
      {/* Main Card */}
      <div className="bg-gradient-to-br from-purple-50 to-indigo-50 rounded-2xl p-6 border-2 border-purple-200 shadow-lg">
        {/* Header */}
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-6">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 bg-gradient-to-br from-purple-500 to-indigo-600 rounded-2xl flex items-center justify-center">
              <span className="text-3xl">👤</span>
            </div>
            <div>
              <h2 className="text-2xl font-bold text-gray-900">{portfolio.name}</h2>
              <div className="flex items-center gap-2 mt-1">
                <span className={`px-3 py-1 rounded-full text-xs font-semibold ${getRiskBadgeColor(portfolio.riskLevel)}`}>
                  Risco {portfolio.riskLevel}
                </span>
                <span className="text-sm text-gray-500">
                  {recommendedAssets.length} ativos
                </span>
              </div>
            </div>
          </div>

          <button
            onClick={onRegenerate}
            className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors flex items-center gap-2"
          >
            <span>🔄</span>
            <span>Gerar Nova Carteira</span>
          </button>
        </div>

        {/* AI Analysis */}
        {portfolio.description && (
          <div className="bg-white/60 backdrop-blur rounded-xl p-4 mb-6 border border-purple-100">
            <div className="flex items-start gap-3">
              <span className="text-2xl">🤖</span>
              <div>
                <h4 className="font-semibold text-gray-800 mb-1">Analise da IA</h4>
                <p className="text-sm text-gray-600">{portfolio.description}</p>
                {portfolio.strategy && (
                  <p className="text-sm text-purple-700 mt-2 font-medium">{portfolio.strategy}</p>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Suggested Composition */}
        <div className="mb-6">
          <h4 className="text-sm font-semibold text-gray-800 mb-3">Composicao da Carteira:</h4>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {portfolio.suggestedComposition?.map((asset, idx) => (
              <div
                key={idx}
                className="bg-white rounded-lg p-3 border border-purple-100"
              >
                <div className="flex justify-between items-center mb-1">
                  <span className="text-xs text-gray-600">{asset.type}</span>
                  <span className="text-sm font-bold text-purple-600">{asset.percentage}%</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className="bg-gradient-to-r from-purple-500 to-indigo-500 h-2 rounded-full transition-all"
                    style={{ width: `${asset.percentage}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Characteristics */}
        {portfolio.characteristics && portfolio.characteristics.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {portfolio.characteristics.map((char, idx) => (
              <span
                key={idx}
                className="px-3 py-1 bg-purple-100 text-purple-700 rounded-full text-xs font-medium"
              >
                {char}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Distribuição de um valor pelos ativos */}
      <div className="bg-white rounded-xl p-6 border border-indigo-200 shadow-sm">
        <h4 className="text-lg font-semibold text-indigo-700 mb-3">Simular aporte</h4>
        <div className="flex flex-wrap items-center gap-3 mb-4">
          <span className="text-sm text-gray-600">Quanto você quer investir?</span>
          <input
            type="text"
            inputMode="decimal"
            value={investInput}
            onChange={(e) => setInvestInput(e.target.value)}
            placeholder="10.000,00"
            className="w-40 px-3 py-2 text-sm border-2 border-indigo-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-400/50"
          />
        </div>
        {investAmount > 0 && (() => {
          const totalAlloc = goodPriceAssets.reduce((s, a) => s + (a.targetAllocation || 0), 0) || 1;
          let usado = 0;
          const rows = goodPriceAssets.map((a) => {
            const valor = (investAmount * (a.targetAllocation || 0)) / totalAlloc;
            const temPreco = a.currentPrice != null && a.currentPrice > 0;
            const qtde = temPreco ? Math.floor(valor / (a.currentPrice as number)) : null;
            const aplicado = temPreco ? (qtde as number) * (a.currentPrice as number) : valor;
            usado += aplicado;
            return { a, valor, qtde, aplicado };
          });
          const sobra = investAmount - usado;
          return (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs text-gray-500 uppercase border-b">
                    <th className="py-2 pr-3">Ativo</th>
                    <th className="py-2 pr-3 text-right">%</th>
                    <th className="py-2 pr-3 text-right">Destinar</th>
                    <th className="py-2 pr-3 text-right">Qtde</th>
                    <th className="py-2 text-right">Aplicado</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map(({ a, valor, qtde, aplicado }) => (
                    <tr key={a.ticker} className="border-b last:border-0">
                      <td className="py-2 pr-3 font-medium text-gray-800">{a.ticker}</td>
                      <td className="py-2 pr-3 text-right text-gray-600">{a.targetAllocation}%</td>
                      <td className="py-2 pr-3 text-right">{fmtBRL(valor)}</td>
                      <td className="py-2 pr-3 text-right">{qtde != null ? qtde : '—'}</td>
                      <td className="py-2 text-right font-semibold">{fmtBRL(aplicado)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <p className="text-xs text-gray-500 mt-3">
                Sobra em caixa (cotas inteiras): <strong>{fmtBRL(sobra)}</strong>.
                Renda fixa não tem cotação — o valor destinado é aplicado direto.
              </p>
            </div>
          );
        })()}
      </div>

      {/* Assets - Good Price */}
      {goodPriceAssets.length > 0 && (
        <div className="bg-white rounded-xl p-6 border border-green-200 shadow-sm">
          <h4 className="text-lg font-semibold text-green-700 mb-4 flex items-center gap-2">
            <span className="w-3 h-3 bg-green-500 rounded-full"></span>
            Ativos Recomendados para Compra ({goodPriceAssets.length})
          </h4>
          <AssetTable
            assets={goodPriceAssets}
            showDY={true}
            isFixedIncome={false}
            onAssetSelect={setSelectedAsset}
          />
        </div>
      )}

      {/* Assets - Above Ceiling */}
      {aboveCeilingAssets.length > 0 && (
        <div className="bg-white rounded-xl p-6 border border-orange-200 shadow-sm">
          <h4 className="text-lg font-semibold text-orange-700 mb-3 flex items-center gap-2">
            <span className="w-3 h-3 bg-orange-500 rounded-full"></span>
            Aguardando Melhor Preco ({aboveCeilingAssets.length})
          </h4>
          <p className="text-sm text-orange-600 mb-4">
            Estes ativos estao acima do preco-teto. Aguarde uma correcao para comprar.
          </p>
          <AssetTable
            assets={aboveCeilingAssets}
            showDY={true}
            isFixedIncome={false}
            onAssetSelect={setSelectedAsset}
          />
        </div>
      )}

      {/* No Assets Message */}
      {recommendedAssets.length === 0 && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-6 text-center">
          <span className="text-4xl block mb-3">📭</span>
          <p className="text-yellow-800">
            Nenhum ativo selecionado para esta carteira.
          </p>
          <button
            onClick={onRegenerate}
            className="mt-4 px-4 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700"
          >
            Tentar Novamente
          </button>
        </div>
      )}

      {/* Asset Detail Modal */}
      {selectedAsset && (
        <AssetDetailModal
          asset={selectedAsset}
          portfolioName={portfolio.name}
          onClose={() => setSelectedAsset(null)}
        />
      )}
    </div>
  );
};

export default MyPortfolioCard;
