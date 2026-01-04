import { useState } from 'react';
import type { IdentifiedTransaction } from '../types';

interface ExtractReviewProps {
  transactions: IdentifiedTransaction[];
  onBack: () => void;
  onSave: (transactions: IdentifiedTransaction[]) => void;
  loading: boolean;
  error: string | null;
}

const ExtractReview = ({ transactions, onBack, onSave, loading, error }: ExtractReviewProps) => {
  const [selectedTransactions, setSelectedTransactions] = useState<Set<number>>(
    new Set(transactions.map((_, index) => index))
  );

  const toggleTransaction = (index: number) => {
    const newSelected = new Set(selectedTransactions);
    if (newSelected.has(index)) {
      newSelected.delete(index);
    } else {
      newSelected.add(index);
    }
    setSelectedTransactions(newSelected);
  };

  const handleSave = () => {
    if (selectedTransactions.size === 0) return;
    const transactionsToSave = transactions.filter((_, index) => selectedTransactions.has(index));
    onSave(transactionsToSave);
  };

  const formatCurrency = (value: number): string => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  };

  const formatDate = (dateString: string): string => {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('pt-BR');
    } catch {
      return dateString;
    }
  };


  return (
    <div className="max-w-5xl mx-auto p-6">
      <div className="bg-white/80 backdrop-blur-sm rounded-2xl shadow-lg border-2 border-slate-200/60 p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold text-slate-800">
            Revisar Transações Identificadas
          </h2>
          <button
            onClick={onBack}
            className="px-4 py-2 border-2 border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors font-semibold"
          >
            Voltar
          </button>
        </div>

        <div className="mb-4 text-sm text-slate-600">
          <p>
            <strong>{transactions.length}</strong> transação(ões) identificada(s) pela IA
          </p>
          <p className="mt-1">
            Selecione as transações que deseja salvar (selecionadas: {selectedTransactions.size})
          </p>
        </div>

        <div className="overflow-x-auto mb-6">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-bold text-slate-700 uppercase">
                  <input
                    type="checkbox"
                    checked={selectedTransactions.size === transactions.length}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedTransactions(new Set(transactions.map((_, i) => i)));
                      } else {
                        setSelectedTransactions(new Set());
                      }
                    }}
                    className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                  />
                </th>
                <th className="px-4 py-3 text-left text-xs font-bold text-slate-700 uppercase">
                  Descrição
                </th>
                <th className="px-4 py-3 text-left text-xs font-bold text-slate-700 uppercase">
                  Data
                </th>
                <th className="px-4 py-3 text-left text-xs font-bold text-slate-700 uppercase">
                  Valor
                </th>
                <th className="px-4 py-3 text-left text-xs font-bold text-slate-700 uppercase">
                  Tipo
                </th>
                <th className="px-4 py-3 text-left text-xs font-bold text-slate-700 uppercase">
                  Confiança
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-slate-200">
              {transactions.map((transaction, index) => (
                <tr
                  key={index}
                  className={`hover:bg-slate-50 ${
                    selectedTransactions.has(index) ? 'bg-blue-50' : ''
                  }`}
                >
                  <td className="px-4 py-3">
                    <input
                      type="checkbox"
                      checked={selectedTransactions.has(index)}
                      onChange={() => toggleTransaction(index)}
                      className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                    />
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-900">
                    {transaction.description}
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-600">
                    {formatDate(transaction.date)}
                  </td>
                  <td className="px-4 py-3 text-sm font-semibold text-slate-900">
                    {formatCurrency(transaction.amount)}
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-700">
                    {transaction.expenseTypeName}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {error && (
          <div className="mb-4 p-4 rounded-lg border-2 bg-red-50 border-red-200 text-red-700">
            {error}
          </div>
        )}

        <div className="flex gap-3">
          <button
            onClick={handleSave}
            disabled={loading || selectedTransactions.size === 0}
            className="flex-1 px-4 py-3 bg-gradient-to-r from-emerald-600 to-emerald-700 text-white rounded-lg hover:from-emerald-700 hover:to-emerald-800 transition-all disabled:opacity-50 disabled:cursor-not-allowed font-semibold shadow-md hover:shadow-lg"
          >
            {loading ? 'Salvando...' : `Salvar ${selectedTransactions.size} Transação(ões)`}
          </button>
          <button
            onClick={onBack}
            className="px-4 py-3 border-2 border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors font-semibold"
          >
            Cancelar
          </button>
        </div>
      </div>
    </div>
  );
};

export default ExtractReview;

