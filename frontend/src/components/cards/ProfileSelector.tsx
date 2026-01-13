import React from 'react';
import type { RiskProfile, RiskProfileOption } from '../../types';

interface ProfileSelectorProps {
  onSelect: (profile: RiskProfile) => void;
  isLoading?: boolean;
}

const RISK_PROFILES: RiskProfileOption[] = [
  {
    id: 'CONSERVADOR',
    name: 'Conservador',
    icon: '🛡️',
    description: 'Foco em renda passiva e baixa volatilidade',
    allocation: '60% FIIs, 30% Dividendos, 10% Renda Fixa',
    color: 'from-green-500 to-emerald-600',
  },
  {
    id: 'MODERADO',
    name: 'Moderado',
    icon: '⚖️',
    description: 'Equilibrio entre crescimento e renda',
    allocation: '40% Valor, 30% FIIs, 20% Dividendos, 10% Cripto',
    color: 'from-blue-500 to-indigo-600',
  },
  {
    id: 'ARROJADO',
    name: 'Arrojado',
    icon: '🚀',
    description: 'Foco em valorizacao e maior potencial',
    allocation: '40% Acoes, 25% Small Caps, 20% Cripto, 15% Internacional',
    color: 'from-orange-500 to-red-600',
  },
];

const ProfileSelector: React.FC<ProfileSelectorProps> = ({ onSelect, isLoading }) => {
  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center">
        <span className="text-6xl mb-4 block">👤</span>
        <h2 className="text-2xl font-bold text-gray-900">
          Monte sua Carteira Personalizada
        </h2>
        <p className="text-gray-600 mt-2 max-w-xl mx-auto">
          Escolha seu perfil de investidor e a IA vai selecionar os melhores ativos
          das nossas carteiras recomendadas, priorizando aqueles com bom preco de entrada.
        </p>
      </div>

      {/* Profile Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-4xl mx-auto">
        {RISK_PROFILES.map((profile) => (
          <button
            key={profile.id}
            onClick={() => onSelect(profile.id)}
            disabled={isLoading}
            className={`
              p-6 rounded-2xl bg-gradient-to-br ${profile.color} text-white
              transform transition-all duration-200
              hover:scale-105 hover:shadow-xl
              disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100
              text-left
            `}
          >
            <span className="text-5xl block mb-4">{profile.icon}</span>
            <h3 className="text-xl font-bold mb-2">{profile.name}</h3>
            <p className="text-sm opacity-90 mb-4">{profile.description}</p>
            <div className="bg-white/20 rounded-lg p-3">
              <p className="text-xs font-medium">Composicao:</p>
              <p className="text-xs opacity-80 mt-1">{profile.allocation}</p>
            </div>
          </button>
        ))}
      </div>

      {/* Info Box */}
      <div className="bg-indigo-50 border border-indigo-200 rounded-xl p-4 max-w-2xl mx-auto">
        <div className="flex items-start gap-3">
          <span className="text-2xl">🤖</span>
          <div>
            <h4 className="font-semibold text-indigo-900">Como funciona?</h4>
            <ul className="text-sm text-indigo-700 mt-2 space-y-1">
              <li>1. A IA analisa todos os ativos das 6 carteiras recomendadas</li>
              <li>2. Seleciona apenas ativos com preco atual abaixo do teto</li>
              <li>3. Monta uma carteira diversificada baseada no seu perfil</li>
              <li>4. Sua carteira sera revisada automaticamente a cada 10 dias</li>
            </ul>
          </div>
        </div>
      </div>

      {/* Loading State */}
      {isLoading && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl p-8 text-center max-w-sm mx-4">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto mb-4"></div>
            <h3 className="text-lg font-semibold text-gray-900">Montando sua carteira...</h3>
            <p className="text-sm text-gray-600 mt-2">
              A IA esta analisando os melhores ativos para voce
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProfileSelector;
