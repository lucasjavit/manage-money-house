import React, { useState } from 'react';
import type { InvestmentPortfolio, RecommendedAsset } from '../../types';
import AssetTable from './AssetTable';
import AssetDetailModal from './AssetDetailModal';

interface PortfolioCardProps {
  portfolio: InvestmentPortfolio;
}

const PortfolioCard: React.FC<PortfolioCardProps> = ({ portfolio }) => {
  const [selectedAsset, setSelectedAsset] = useState<RecommendedAsset | null>(null);
  const [showDetails, setShowDetails] = useState(false);

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

      {/* Toggle para mostrar Estratégia e Características */}
      {(portfolio.strategy || (portfolio.characteristics && portfolio.characteristics.length > 0)) && (
        <button
          onClick={() => setShowDetails(!showDetails)}
          className="flex items-center gap-2 text-sm text-indigo-600 hover:text-indigo-800 mb-4 transition-colors"
        >
          <svg
            className={`w-4 h-4 transition-transform ${showDetails ? 'rotate-180' : ''}`}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
          <span>{showDetails ? 'Ocultar detalhes' : 'Ver estratégia e características'}</span>
        </button>
      )}

      {/* Seção colapsável: Estratégia e Características */}
      {showDetails && (
        <div className="mb-4 bg-white/50 rounded-lg p-4 border border-indigo-100">
          {portfolio.strategy && (
            <div className="mb-3">
              <h4 className="text-sm font-semibold text-gray-800 mb-1">Estratégia:</h4>
              <p className="text-sm text-gray-600">{portfolio.strategy}</p>
            </div>
          )}
          {portfolio.characteristics && portfolio.characteristics.length > 0 && (
            <div>
              <h4 className="text-sm font-semibold text-gray-800 mb-2">Características:</h4>
              <div className="flex flex-wrap gap-2">
                {portfolio.characteristics.map((char, idx) => (
                  <span
                    key={idx}
                    className="px-3 py-1 bg-indigo-100 text-indigo-700 rounded-full text-xs font-medium"
                  >
                    {char}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

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

      {/* Ativos com Bom Preço (Comprar) */}
      {goodPriceAssets.length > 0 && (
        <div className="mt-4">
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
