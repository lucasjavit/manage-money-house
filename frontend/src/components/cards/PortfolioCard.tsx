import React, { useState } from 'react';
import type { InvestmentPortfolio, RecommendedAsset } from '../../types';
import AssetTable from './AssetTable';
import AssetDetailModal from './AssetDetailModal';

interface PortfolioCardProps {
  portfolio: InvestmentPortfolio;
}

const PortfolioCard: React.FC<PortfolioCardProps> = ({ portfolio }) => {
  const [selectedAsset, setSelectedAsset] = useState<RecommendedAsset | null>(null);

  // DY visível apenas para carteiras focadas em dividendos/valor
  const showDY =
    portfolio.name.includes('Dividendos') ||
    portfolio.name.includes('Valor') ||
    portfolio.name.includes('Small Caps');

  // Renda Fixa tem tabela simplificada (sem preços/viés)
  const isFixedIncome = portfolio.name.includes('Renda Fixa');

  // Separar ativos em duas listas: bom preço vs acima do teto
  const recommendedAssets = portfolio.recommendedAssets || [];

  // Ativos com bom preço (preço atual <= preço teto) ou sem preço definido
  const goodPriceAssets = recommendedAssets.filter((asset) => {
    // Se não tem preço atual ou preço teto, mantém na lista principal
    if (asset.currentPrice == null || asset.ceilingPrice == null) {
      return true;
    }
    return asset.currentPrice <= asset.ceilingPrice;
  });

  // Ativos acima do teto (preço atual > preço teto)
  const aboveCeilingAssets = recommendedAssets.filter((asset) => {
    if (asset.currentPrice == null || asset.ceilingPrice == null) {
      return false;
    }
    return asset.currentPrice > asset.ceilingPrice;
  });

  return (
    <div className="bg-gradient-to-br from-indigo-50 to-purple-50 rounded-xl p-6 border border-indigo-200 shadow-sm hover:shadow-md transition-shadow">
      <div className="flex items-center gap-3 mb-4">
        <span className="text-4xl">{portfolio.icon}</span>
        <div>
          <h3 className="text-xl font-bold text-gray-900">{portfolio.name}</h3>
          <p className="text-sm text-gray-600">Risco: {portfolio.riskLevel}</p>
        </div>
      </div>

      <p className="text-sm text-gray-700 mb-4">{portfolio.description}</p>

      <div className="mb-4">
        <h4 className="text-sm font-semibold text-gray-800 mb-2">
          Composição Sugerida:
        </h4>
        <div className="space-y-2">
          {portfolio.suggestedComposition.map((asset, idx) => (
            <div key={idx}>
              <div className="flex justify-between text-xs mb-1">
                <span className="text-gray-700">{asset.description}</span>
                <span className="font-semibold text-gray-900">
                  {asset.percentage}%
                </span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className="bg-indigo-600 h-2 rounded-full transition-all"
                  style={{ width: `${asset.percentage}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="mb-4">
        <h4 className="text-sm font-semibold text-gray-800 mb-2">Estratégia:</h4>
        <p className="text-xs text-gray-600">{portfolio.strategy}</p>
      </div>

      <div>
        <h4 className="text-sm font-semibold text-gray-800 mb-2">
          Características:
        </h4>
        <ul className="text-xs text-gray-600 space-y-1">
          {portfolio.characteristics.map((char, idx) => (
            <li key={idx}>• {char}</li>
          ))}
        </ul>
      </div>

      {/* Ativos com Bom Preço (Comprar) */}
      {goodPriceAssets.length > 0 && (
        <div className="mt-4 pt-4 border-t border-indigo-200">
          <h4 className="text-sm font-semibold text-green-700 mb-3 flex items-center gap-2">
            <span className="w-2 h-2 bg-green-500 rounded-full"></span>
            Ativos Recomendados para Compra ({goodPriceAssets.length})
          </h4>
          <AssetTable
            assets={goodPriceAssets}
            showDY={showDY}
            isFixedIncome={isFixedIncome}
            onAssetSelect={setSelectedAsset}
          />
        </div>
      )}

      {/* Ativos Acima do Teto (Aguardar) */}
      {aboveCeilingAssets.length > 0 && (
        <div className="mt-4 pt-4 border-t border-orange-200">
          <h4 className="text-sm font-semibold text-orange-700 mb-3 flex items-center gap-2">
            <span className="w-2 h-2 bg-orange-500 rounded-full"></span>
            Aguardando Melhor Preco ({aboveCeilingAssets.length})
          </h4>
          <p className="text-xs text-orange-600 mb-3">
            Estes ativos estao acima do preco-teto. Aguarde uma correcao para comprar.
          </p>
          <AssetTable
            assets={aboveCeilingAssets}
            showDY={showDY}
            isFixedIncome={isFixedIncome}
            onAssetSelect={setSelectedAsset}
          />
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

export default PortfolioCard;
