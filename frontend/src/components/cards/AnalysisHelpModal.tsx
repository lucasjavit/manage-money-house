import React from 'react';

interface Props {
  onClose: () => void;
}

const AnalysisHelpModal: React.FC<Props> = ({ onClose }) => {
  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="bg-white rounded-2xl max-w-3xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-600 to-cyan-600 p-6">
          <div className="flex justify-between items-start">
            <div>
              <h2 className="text-2xl font-bold text-white">Como Funciona a Analise?</h2>
              <p className="text-blue-200 mt-1">Entenda os criterios de avaliacao dos seus ativos</p>
            </div>
            <button
              onClick={onClose}
              className="text-white hover:bg-white/20 p-2 rounded-lg transition-colors"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        {/* Content - scrollable */}
        <div className="flex-1 overflow-y-auto p-6 space-y-8">

          {/* Secao 1: Dados Fundamentalistas */}
          <section>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                <span className="text-xl">📊</span>
              </div>
              <h3 className="text-lg font-bold text-gray-800">Dados Fundamentalistas em Tempo Real</h3>
            </div>
            <p className="text-gray-600 mb-4">
              Buscamos dados diretamente do <span className="font-semibold text-blue-600">Yahoo Finance</span> para cada acao e FII da sua carteira:
            </p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-lg">📈</span>
                  <span className="font-semibold text-gray-800">P/L (Preco/Lucro)</span>
                </div>
                <p className="text-sm text-gray-600">
                  Indica quantos anos de lucro o preco atual representa.
                  <span className="block mt-1 text-blue-600 font-medium">Menor = mais barato</span>
                </p>
                <div className="mt-2 text-xs text-gray-500">
                  Ideal: Bancos ~8x | Varejo ~15x | Tech ~20x
                </div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-lg">📚</span>
                  <span className="font-semibold text-gray-800">P/VP (Preco/Valor Patrimonial)</span>
                </div>
                <p className="text-sm text-gray-600">
                  Compara o preco com o patrimonio liquido da empresa.
                  <span className="block mt-1 text-green-600 font-medium">Abaixo de 1.0 = desconto</span>
                </p>
                <div className="mt-2 text-xs text-gray-500">
                  Negociando abaixo do patrimonio e um bom sinal
                </div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-lg">💰</span>
                  <span className="font-semibold text-gray-800">Dividend Yield (DY)</span>
                </div>
                <p className="text-sm text-gray-600">
                  Percentual de dividendos pagos sobre o preco atual.
                  <span className="block mt-1 text-purple-600 font-medium">Acima de 6% = excelente</span>
                </p>
                <div className="mt-2 text-xs text-gray-500">
                  Otimo para quem busca renda passiva
                </div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-lg">🎯</span>
                  <span className="font-semibold text-gray-800">LPA (Lucro por Acao)</span>
                </div>
                <p className="text-sm text-gray-600">
                  Usado para calcular o preco teto do ativo.
                  <span className="block mt-1 text-amber-600 font-medium">Preco Teto = LPA x P/L justo</span>
                </p>
                <div className="mt-2 text-xs text-gray-500">
                  Acima do teto = caro | Abaixo = oportunidade
                </div>
              </div>
            </div>
          </section>

          {/* Secao 2: Regras de Valuation */}
          <section>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
                <span className="text-xl">⚖️</span>
              </div>
              <h3 className="text-lg font-bold text-gray-800">Criterios de Valuation</h3>
            </div>

            {/* Tabela de Acoes */}
            <div className="mb-4">
              <h4 className="font-semibold text-gray-700 mb-2 flex items-center gap-2">
                <span className="w-6 h-6 bg-blue-500 rounded text-white text-xs flex items-center justify-center">A</span>
                Acoes
              </h4>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="bg-gray-100">
                      <th className="px-3 py-2 text-left font-medium text-gray-600">Metrica</th>
                      <th className="px-3 py-2 text-center font-medium text-green-600">Barato</th>
                      <th className="px-3 py-2 text-center font-medium text-gray-600">Justo</th>
                      <th className="px-3 py-2 text-center font-medium text-red-600">Caro</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-b">
                      <td className="px-3 py-2 font-medium">P/L</td>
                      <td className="px-3 py-2 text-center text-green-600">&lt; 8</td>
                      <td className="px-3 py-2 text-center">8 - 15</td>
                      <td className="px-3 py-2 text-center text-red-600">&gt; 15</td>
                    </tr>
                    <tr>
                      <td className="px-3 py-2 font-medium">P/VP</td>
                      <td className="px-3 py-2 text-center text-green-600">&lt; 1.0</td>
                      <td className="px-3 py-2 text-center">0.8 - 1.5</td>
                      <td className="px-3 py-2 text-center text-red-600">&gt; 2.0</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            {/* Tabela de FIIs */}
            <div className="mb-4">
              <h4 className="font-semibold text-gray-700 mb-2 flex items-center gap-2">
                <span className="w-6 h-6 bg-purple-500 rounded text-white text-xs flex items-center justify-center">F</span>
                Fundos Imobiliarios (FIIs)
              </h4>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="bg-gray-100">
                      <th className="px-3 py-2 text-left font-medium text-gray-600">Metrica</th>
                      <th className="px-3 py-2 text-center font-medium text-green-600">Barato</th>
                      <th className="px-3 py-2 text-center font-medium text-gray-600">Justo</th>
                      <th className="px-3 py-2 text-center font-medium text-red-600">Caro</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-b">
                      <td className="px-3 py-2 font-medium">P/VP</td>
                      <td className="px-3 py-2 text-center text-green-600">&lt; 0.9</td>
                      <td className="px-3 py-2 text-center">0.9 - 1.1</td>
                      <td className="px-3 py-2 text-center text-red-600">&gt; 1.1</td>
                    </tr>
                    <tr>
                      <td className="px-3 py-2 font-medium">DY (anual)</td>
                      <td className="px-3 py-2 text-center text-green-600">&gt; 10%</td>
                      <td className="px-3 py-2 text-center">8% - 10%</td>
                      <td className="px-3 py-2 text-center text-red-600">&lt; 8%</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            {/* Renda Fixa */}
            <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
              <h4 className="font-semibold text-amber-800 mb-2 flex items-center gap-2">
                <span className="text-lg">⚠️</span>
                Renda Fixa
              </h4>
              <ul className="text-sm text-amber-700 space-y-1">
                <li>• <span className="font-semibold text-red-600">Bancos em liquidacao</span> (Master, BRK, Portocred) → VENDER imediatamente</li>
                <li>• Demais CDBs, LCAs, LCIs → MANTER ate o vencimento</li>
                <li>• Vencimento &lt; 6 meses → Risco BAIXO</li>
                <li>• Vencimento &gt; 2 anos → Risco MEDIO</li>
              </ul>
            </div>
          </section>

          {/* Secao 3: Health Score */}
          <section>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                <span className="text-xl">💚</span>
              </div>
              <h3 className="text-lg font-bold text-gray-800">Health Score da Carteira</h3>
            </div>
            <p className="text-gray-600 mb-4">
              Score de <span className="font-bold">0 a 100 pontos</span> dividido em 4 dimensoes de 25 pontos cada:
            </p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="bg-blue-50 rounded-lg p-4 border border-blue-200">
                <div className="flex justify-between items-center mb-2">
                  <span className="font-semibold text-blue-800">Diversificacao</span>
                  <span className="text-sm text-blue-600">0-25 pts</span>
                </div>
                <p className="text-sm text-blue-700">
                  Quantas classes de ativos voce tem (acoes, FIIs, renda fixa, fundos)
                </p>
              </div>

              <div className="bg-purple-50 rounded-lg p-4 border border-purple-200">
                <div className="flex justify-between items-center mb-2">
                  <span className="font-semibold text-purple-800">Concentracao</span>
                  <span className="text-sm text-purple-600">0-25 pts</span>
                </div>
                <p className="text-sm text-purple-700">
                  Se algum ativo representa muito da carteira (ideal: nenhum &gt; 10%)
                </p>
              </div>

              <div className="bg-green-50 rounded-lg p-4 border border-green-200">
                <div className="flex justify-between items-center mb-2">
                  <span className="font-semibold text-green-800">Qualidade</span>
                  <span className="text-sm text-green-600">0-25 pts</span>
                </div>
                <p className="text-sm text-green-700">
                  Baseado nas recomendacoes individuais (MANTER/COMPRAR = bom)
                </p>
              </div>

              <div className="bg-amber-50 rounded-lg p-4 border border-amber-200">
                <div className="flex justify-between items-center mb-2">
                  <span className="font-semibold text-amber-800">Risco</span>
                  <span className="text-sm text-amber-600">0-25 pts</span>
                </div>
                <p className="text-sm text-amber-700">
                  Nivel de risco geral dos ativos (mais ativos de baixo risco = maior score)
                </p>
              </div>
            </div>

            {/* Status do Score */}
            <div className="mt-4 flex flex-wrap gap-2 justify-center">
              <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-sm font-medium">85-100: Excelente 🌟</span>
              <span className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm font-medium">70-84: Bom 👍</span>
              <span className="px-3 py-1 bg-yellow-100 text-yellow-800 rounded-full text-sm font-medium">50-69: Regular ⚠️</span>
              <span className="px-3 py-1 bg-orange-100 text-orange-800 rounded-full text-sm font-medium">30-49: Ruim ⚡</span>
              <span className="px-3 py-1 bg-red-100 text-red-800 rounded-full text-sm font-medium">0-29: Critico 🚨</span>
            </div>
          </section>

          {/* Secao 4: Recomendacoes */}
          <section>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-indigo-100 rounded-lg flex items-center justify-center">
                <span className="text-xl">🎯</span>
              </div>
              <h3 className="text-lg font-bold text-gray-800">Recomendacoes</h3>
            </div>
            <div className="space-y-3">
              <div className="flex items-start gap-3 p-4 bg-green-50 rounded-lg border border-green-200">
                <span className="px-3 py-1 bg-green-500 text-white rounded-full text-sm font-bold">MANTER</span>
                <div>
                  <p className="font-medium text-green-800">Ativo bem posicionado</p>
                  <p className="text-sm text-green-700">Continue com este ativo na carteira. Fundamentos solidos.</p>
                </div>
              </div>

              <div className="flex items-start gap-3 p-4 bg-blue-50 rounded-lg border border-blue-200">
                <span className="px-3 py-1 bg-blue-500 text-white rounded-full text-sm font-bold">COMPRAR_MAIS</span>
                <div>
                  <p className="font-medium text-blue-800">Oportunidade de compra</p>
                  <p className="text-sm text-blue-700">Ativo subvalorizado! Considere aumentar a posicao.</p>
                </div>
              </div>

              <div className="flex items-start gap-3 p-4 bg-red-50 rounded-lg border border-red-200">
                <span className="px-3 py-1 bg-red-500 text-white rounded-full text-sm font-bold">VENDER</span>
                <div>
                  <p className="font-medium text-red-800">Considere realizar ou realocar</p>
                  <p className="text-sm text-red-700">Ativo caro ou com risco elevado. Avalie vender.</p>
                </div>
              </div>
            </div>

            {/* Regra de Concentracao */}
            <div className="mt-4 bg-gray-100 rounded-lg p-4 border-l-4 border-gray-400">
              <p className="text-sm text-gray-700">
                <span className="font-bold">Regra de Concentracao:</span> Se um ativo representa mais de <span className="font-bold text-red-600">20%</span> da carteira,
                a recomendacao sera VENDER independente dos fundamentos, para reduzir o risco de concentracao.
              </p>
            </div>
          </section>

        </div>

        {/* Footer */}
        <div className="border-t border-gray-200 p-4 bg-gray-50">
          <p className="text-center text-sm text-gray-500">
            Dados atualizados em tempo real via Yahoo Finance. Analise por inteligencia artificial.
          </p>
        </div>
      </div>
    </div>
  );
};

export default AnalysisHelpModal;
