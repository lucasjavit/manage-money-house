import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { marketService } from '../services/marketService';
import { portfolioReviewService } from '../services/portfolioReviewService';
import { realPortfolioService } from '../services/realPortfolioService';
import type { MarketDataDashboard, InvestmentPortfolio, PortfolioAnalysis, PortfolioReviewStatus, RiskProfile, RealPortfolioSummary, B3ReportUploadResponse } from '../types';
import MarketIndexCard from './cards/MarketIndexCard';
import PortfolioCard from './cards/PortfolioCard';
import PortfolioReviewModal from './cards/PortfolioReviewModal';
import ProfileSelector from './cards/ProfileSelector';
import MyPortfolioCard from './cards/MyPortfolioCard';
import RealPortfolioUpload from './cards/RealPortfolioUpload';
import RealPortfolioCard from './cards/RealPortfolioCard';

// Definição estática das abas de carteiras (sem dados carregados)
const PORTFOLIO_TABS = [
  { id: 'valor', name: 'Valor', icon: '💎' },
  { id: 'dividendos', name: 'Dividendos', icon: '💰' },
  { id: 'renda-fixa', name: 'Renda Fixa', icon: '🏦' },
  { id: 'internacional', name: 'Internacional', icon: '🌎' },
  { id: 'small-caps', name: 'Small Caps', icon: '🚀' },
  { id: 'cripto', name: 'Criptomoedas', icon: '₿' },
];

const InvestmentPage = () => {
  const [activeTab, setActiveTab] = useState<string>('indices');
  const [marketData, setMarketData] = useState<MarketDataDashboard | null>(null);
  const [portfolios, setPortfolios] = useState<InvestmentPortfolio[]>([]);
  const [loadedTabs, setLoadedTabs] = useState<Set<string>>(new Set());
  const [loadingTab, setLoadingTab] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Portfolio Review State
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [reviewAnalyses, setReviewAnalyses] = useState<PortfolioAnalysis[]>([]);
  const [isRunningReview, setIsRunningReview] = useState(false);
  const [reviewStatus, setReviewStatus] = useState<string>('');
  const [lastReviewStatus, setLastReviewStatus] = useState<PortfolioReviewStatus | null>(null);

  // My Portfolio State (Gerador de Carteira)
  const [myPortfolio, setMyPortfolio] = useState<InvestmentPortfolio | null>(null);
  const [isGeneratingPortfolio, setIsGeneratingPortfolio] = useState(false);
  const [myPortfolioLoaded, setMyPortfolioLoaded] = useState(false);

  // Real Portfolio State (Minha Carteira - Upload B3)
  const [realPortfolio, setRealPortfolio] = useState<RealPortfolioSummary | null>(null);
  const [realPortfolioLoaded, setRealPortfolioLoaded] = useState(false);

  // Obter userId do usuario autenticado
  const { user } = useAuth();
  const currentUserId = user?.id ?? 1;

  // Carregar dados de índices e status do review ao montar o componente
  useEffect(() => {
    loadMarketData();
    loadReviewStatus();
  }, []);

  // Resetar carteira real quando usuario mudar
  useEffect(() => {
    setRealPortfolio(null);
    setRealPortfolioLoaded(false);
  }, [currentUserId]);

  // Carregar dados da carteira quando a aba é selecionada
  useEffect(() => {
    if (activeTab.startsWith('portfolio-') && !loadedTabs.has(activeTab)) {
      loadPortfolioData();
    }
    // Carregar "Gerador de Carteira" se a aba for selecionada
    if (activeTab === 'gerador-carteira' && !myPortfolioLoaded) {
      loadMyPortfolio();
    }
    // Carregar "Minha Carteira" (Upload B3) se a aba for selecionada
    if (activeTab === 'minha-carteira' && !realPortfolioLoaded) {
      loadRealPortfolio();
    }
  }, [activeTab, currentUserId, realPortfolioLoaded]);

  const loadMarketData = async () => {
    try {
      setLoadingTab('indices');
      setError(null);
      const dashboardData = await marketService.getDashboard();
      setMarketData(dashboardData);
      setLoadedTabs((prev) => new Set(prev).add('indices'));
    } catch (err) {
      console.error('Error loading market data:', err);
      setError('Não foi possível carregar os dados de mercado.');
    } finally {
      setLoadingTab(null);
    }
  };

  const loadPortfolioData = async () => {
    try {
      setLoadingTab(activeTab);
      setError(null);

      // Se ainda não carregou nenhuma carteira, busca todas
      if (portfolios.length === 0) {
        const portfoliosData = await marketService.getPortfolios();
        setPortfolios(portfoliosData);
      }

      setLoadedTabs((prev) => new Set(prev).add(activeTab));
    } catch (err) {
      console.error('Error loading portfolio data:', err);
      setError('Não foi possível carregar os dados da carteira.');
    } finally {
      setLoadingTab(null);
    }
  };

  const loadReviewStatus = async () => {
    try {
      const status = await portfolioReviewService.getStatus();
      setLastReviewStatus(status);
    } catch (err) {
      console.error('Error loading review status:', err);
    }
  };

  const loadExistingAnalyses = async () => {
    try {
      const analyses = await portfolioReviewService.getAllAnalyses();
      setReviewAnalyses(analyses);
    } catch (err) {
      console.error('Error loading analyses:', err);
    }
  };

  const handleRunReview = async () => {
    setShowReviewModal(true);
    setIsRunningReview(true);
    setReviewStatus('Iniciando analise de todas as carteiras...');
    setReviewAnalyses([]);

    try {
      const result = await portfolioReviewService.runFullReview();
      setReviewAnalyses(result.results);
      setReviewStatus('');
      await loadReviewStatus();
    } catch (err) {
      console.error('Error running review:', err);
      setReviewStatus('Erro ao executar analise. Tente novamente.');
    } finally {
      setIsRunningReview(false);
    }
  };

  const handleViewAnalyses = async () => {
    setShowReviewModal(true);
    setIsRunningReview(true);
    setReviewStatus('Carregando analises...');

    try {
      await loadExistingAnalyses();
      setReviewStatus('');
    } catch (err) {
      console.error('Error loading analyses:', err);
      setReviewStatus('Erro ao carregar analises.');
    } finally {
      setIsRunningReview(false);
    }
  };

  const handleTabClick = (tabId: string) => {
    setActiveTab(tabId);
  };

  const loadMyPortfolio = async () => {
    try {
      setLoadingTab('gerador-carteira');
      const portfolio = await marketService.getPersonalizedPortfolio(currentUserId);
      setMyPortfolio(portfolio);
      setMyPortfolioLoaded(true);
    } catch (err) {
      console.error('Error loading my portfolio:', err);
    } finally {
      setLoadingTab(null);
    }
  };

  const loadRealPortfolio = async () => {
    try {
      setLoadingTab('minha-carteira');
      const portfolio = await realPortfolioService.getLatestPortfolio(currentUserId);
      setRealPortfolio(portfolio);
      setRealPortfolioLoaded(true);
    } catch (err) {
      console.error('Error loading real portfolio:', err);
    } finally {
      setLoadingTab(null);
    }
  };

  const handleRealPortfolioUpload = (data: B3ReportUploadResponse) => {
    // Recarregar a carteira apos upload
    loadRealPortfolio();
  };

  const handleNewUpload = () => {
    setRealPortfolio(null);
  };

  const handleGeneratePortfolio = async (riskProfile: RiskProfile) => {
    try {
      setIsGeneratingPortfolio(true);
      const portfolio = await marketService.generatePersonalizedPortfolio(currentUserId, riskProfile);
      setMyPortfolio(portfolio);
    } catch (err) {
      console.error('Error generating portfolio:', err);
      setError('Erro ao gerar carteira. Tente novamente.');
    } finally {
      setIsGeneratingPortfolio(false);
    }
  };

  const handleRegeneratePortfolio = () => {
    setMyPortfolio(null);
  };

  const refreshCurrentTab = async () => {
    if (activeTab === 'indices') {
      await loadMarketData();
    } else {
      // Força recarregar carteiras
      setPortfolios([]);
      setLoadedTabs((prev) => {
        const newSet = new Set(prev);
        // Remove todas as abas de portfolio do set
        PORTFOLIO_TABS.forEach((_, index) => newSet.delete(`portfolio-${index}`));
        return newSet;
      });
      await loadPortfolioData();
    }
  };

  const formatLastUpdate = (timestamp: string) => {
    try {
      const date = new Date(timestamp);
      return date.toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return timestamp;
    }
  };

  const getPortfolioByIndex = (index: number): InvestmentPortfolio | null => {
    return portfolios[index] || null;
  };

  const renderLoading = () => (
    <div className="flex items-center justify-center py-20">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto mb-4"></div>
        <p className="text-gray-600">Carregando dados...</p>
      </div>
    </div>
  );

  const renderError = () => (
    <div className="flex items-center justify-center py-20">
      <div className="text-center">
        <span className="text-6xl mb-4 block">⚠️</span>
        <p className="text-red-600 mb-4">{error}</p>
        <button
          onClick={refreshCurrentTab}
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
        >
          Tentar Novamente
        </button>
      </div>
    </div>
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="text-4xl">📈</span>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Investimentos</h1>
            <p className="text-sm text-gray-600">
              Acompanhe índices do mercado e conheça estratégias de investimento
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {/* Analysis Buttons */}
          <div className="flex items-center gap-2">
            <button
              onClick={handleViewAnalyses}
              className="px-3 py-2 text-sm bg-indigo-100 text-indigo-700 hover:bg-indigo-200 rounded-lg transition-colors flex items-center gap-2"
              title="Ver analises anteriores"
            >
              <span>📊</span>
              <span className="hidden sm:inline">Ver Analises</span>
            </button>
            <button
              onClick={handleRunReview}
              disabled={lastReviewStatus?.isRunning}
              className="px-3 py-2 text-sm bg-gradient-to-r from-indigo-600 to-purple-600 text-white hover:from-indigo-700 hover:to-purple-700 rounded-lg transition-colors flex items-center gap-2 disabled:opacity-50"
              title="Rodar analise de IA em todas as carteiras"
            >
              <span>🤖</span>
              <span className="hidden sm:inline">Rodar Analise IA</span>
            </button>
          </div>
          <button
            onClick={refreshCurrentTab}
            disabled={loadingTab !== null}
            className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50"
            title="Atualizar dados"
          >
            🔄
          </button>
        </div>
      </div>

      {/* Last Review Info */}
      {lastReviewStatus && lastReviewStatus.lastRunTime && (
        <div className="text-sm text-gray-500 bg-gray-50 rounded-lg px-4 py-2 flex items-center gap-2">
          <span>🕒</span>
          <span>Ultima analise: {lastReviewStatus.lastRunStatus}</span>
        </div>
      )}

      {/* Tabs */}
      <div className="flex flex-wrap gap-1 border-b border-gray-200 overflow-x-auto pb-px">
        <button
          onClick={() => handleTabClick('indices')}
          className={`px-4 py-2 text-sm font-semibold transition-colors whitespace-nowrap rounded-t-lg ${
            activeTab === 'indices'
              ? 'text-indigo-600 border-b-2 border-indigo-600 bg-indigo-50'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
          }`}
        >
          📊 Indices
        </button>
        {/* Minha Carteira - Upload B3 */}
        <button
          onClick={() => handleTabClick('minha-carteira')}
          className={`px-4 py-2 text-sm font-semibold transition-colors whitespace-nowrap rounded-t-lg ${
            activeTab === 'minha-carteira'
              ? 'text-purple-600 border-b-2 border-purple-600 bg-purple-50'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
          }`}
        >
          📁 Minha Carteira
        </button>
        {/* Gerador de Carteira - IA */}
        <button
          onClick={() => handleTabClick('gerador-carteira')}
          className={`px-4 py-2 text-sm font-semibold transition-colors whitespace-nowrap rounded-t-lg ${
            activeTab === 'gerador-carteira'
              ? 'text-teal-600 border-b-2 border-teal-600 bg-teal-50'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
          }`}
        >
          🎯 Gerador de Carteira
        </button>
        {PORTFOLIO_TABS.map((tab, index) => (
          <button
            key={tab.id}
            onClick={() => handleTabClick(`portfolio-${index}`)}
            className={`px-4 py-2 text-sm font-semibold transition-colors whitespace-nowrap rounded-t-lg ${
              activeTab === `portfolio-${index}`
                ? 'text-indigo-600 border-b-2 border-indigo-600 bg-indigo-50'
                : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
            }`}
          >
            {tab.icon} {tab.name}
          </button>
        ))}
      </div>

      {/* Minha Carteira - Upload B3 */}
      {activeTab === 'minha-carteira' && (
        <div className="space-y-6">
          {loadingTab === 'minha-carteira' && renderLoading()}
          {!loadingTab && !realPortfolio && (
            <RealPortfolioUpload
              userId={currentUserId}
              onUploadComplete={handleRealPortfolioUpload}
            />
          )}
          {!loadingTab && realPortfolio && (
            <RealPortfolioCard
              portfolio={realPortfolio}
              onNewUpload={handleNewUpload}
              onRefresh={loadRealPortfolio}
            />
          )}
        </div>
      )}

      {/* Gerador de Carteira - IA */}
      {activeTab === 'gerador-carteira' && (
        <div className="space-y-6">
          {loadingTab === 'gerador-carteira' && renderLoading()}
          {!loadingTab && !myPortfolio && (
            <ProfileSelector
              onSelect={handleGeneratePortfolio}
              isLoading={isGeneratingPortfolio}
            />
          )}
          {!loadingTab && myPortfolio && (
            <>
              <div className="bg-teal-50 border border-teal-200 rounded-lg p-4">
                <p className="text-teal-800 text-sm">
                  <strong>🤖 Carteira gerada por IA:</strong> Esta carteira foi montada automaticamente
                  selecionando os melhores ativos das nossas carteiras recomendadas baseado no seu perfil.
                </p>
              </div>
              <MyPortfolioCard
                portfolio={myPortfolio}
                onRegenerate={handleRegeneratePortfolio}
              />
            </>
          )}
        </div>
      )}

      {/* Content */}
      {activeTab === 'indices' && (
        <>
          {loadingTab === 'indices' && renderLoading()}
          {error && activeTab === 'indices' && renderError()}
          {marketData && !loadingTab && (
            <div className="space-y-8">
              {/* Last Update */}
              <div className="text-sm text-gray-500 text-right">
                Última atualização: {formatLastUpdate(marketData.lastUpdate)}
              </div>

              {/* Forex */}
              <div>
                <h2 className="text-xl font-bold text-gray-900 mb-4 flex items-center gap-2">
                  <span>💵</span> Câmbio
                </h2>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <MarketIndexCard {...marketData.forex.usd} format="currency" />
                  <MarketIndexCard {...marketData.forex.eur} format="currency" />
                  <MarketIndexCard {...marketData.forex.gbp} format="currency" />
                  <MarketIndexCard {...marketData.forex.jpy} format="currency" />
                </div>
              </div>

              {/* Brazilian Indices */}
              <div>
                <h2 className="text-xl font-bold text-gray-900 mb-4 flex items-center gap-2">
                  <span>📈</span> Bolsa Brasileira
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <MarketIndexCard {...marketData.brazilianIndices.ibovespa} format="number" />
                  <MarketIndexCard {...marketData.brazilianIndices.ifix} format="number" />
                  <MarketIndexCard {...marketData.brazilianIndices.idiv} format="number" />
                </div>
              </div>

              {/* US Indices */}
              <div>
                <h2 className="text-xl font-bold text-gray-900 mb-4 flex items-center gap-2">
                  <span>🇺🇸</span> Bolsa Americana
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <MarketIndexCard {...marketData.usIndices.sp500} format="number" />
                  <MarketIndexCard {...marketData.usIndices.nasdaq} format="number" />
                  <MarketIndexCard {...marketData.usIndices.dow} format="number" />
                </div>
              </div>

              {/* Crypto */}
              <div>
                <h2 className="text-xl font-bold text-gray-900 mb-4 flex items-center gap-2">
                  <span>₿</span> Criptomoedas
                </h2>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
                  <MarketIndexCard {...marketData.crypto.bitcoin} format="currency" />
                  <MarketIndexCard {...marketData.crypto.ethereum} format="currency" />
                  {marketData.crypto.otherCoins.map((coin) => (
                    <MarketIndexCard key={coin.symbol} {...coin} format="currency" />
                  ))}
                </div>
              </div>
            </div>
          )}
        </>
      )}

      {/* Individual Portfolio Tabs */}
      {PORTFOLIO_TABS.map((tab, index) => {
        if (activeTab !== `portfolio-${index}`) return null;

        const portfolio = getPortfolioByIndex(index);
        const isLoading = loadingTab === `portfolio-${index}`;

        return (
          <div key={tab.id} className="space-y-6">
            {isLoading && renderLoading()}
            {error && !isLoading && renderError()}
            {portfolio && !isLoading && (
              <>
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                  <p className="text-yellow-800 text-sm">
                    <strong>⚠️ Atenção:</strong> Estas são sugestões educacionais baseadas em análise fundamentalista.
                    Consulte um profissional antes de investir. Rentabilidade passada não garante retornos futuros.
                  </p>
                </div>
                <PortfolioCard portfolio={portfolio} />
              </>
            )}
            {!portfolio && !isLoading && !error && (
              <div className="text-center py-10 text-gray-500">
                Clique para carregar os dados desta carteira.
              </div>
            )}
          </div>
        );
      })}

      {/* Portfolio Review Modal */}
      {showReviewModal && (
        <PortfolioReviewModal
          analyses={reviewAnalyses}
          onClose={() => setShowReviewModal(false)}
          isLoading={isRunningReview}
          status={reviewStatus}
        />
      )}
    </div>
  );
};

export default InvestmentPage;
