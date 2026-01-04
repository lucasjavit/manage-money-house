import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import type { ExpenseType, RecurringExpense, RecurringExpenseRequest } from '../types';
import { recurringExpenseService } from '../services/recurringExpenseService';

interface RecurringExpenseModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  expenseTypes: ExpenseType[];
  recurringExpense?: RecurringExpense | null;
}

const RecurringExpenseModal = ({
  isOpen,
  onClose,
  onSuccess,
  expenseTypes,
  recurringExpense,
}: RecurringExpenseModalProps) => {
  const { user } = useAuth();
  const [expenseTypeId, setExpenseTypeId] = useState<number>(0);
  const [monthlyAmount, setMonthlyAmount] = useState<string>('');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    if (isOpen) {
      if (recurringExpense) {
        // Modo edição
        setExpenseTypeId(recurringExpense.expenseTypeId);
        setMonthlyAmount(recurringExpense.monthlyAmount.toString());
        setStartDate(recurringExpense.startDate);
        setEndDate(recurringExpense.endDate);
      } else {
        // Modo criação
        setExpenseTypeId(expenseTypes[0]?.id || 0);
        setMonthlyAmount('');
        setStartDate('');
        setEndDate('');
      }
      setError('');
    }
  }, [isOpen, recurringExpense, expenseTypes]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!expenseTypeId) {
      setError('Selecione um tipo de despesa');
      return;
    }

    if (!monthlyAmount || parseFloat(monthlyAmount) <= 0) {
      setError('Informe um valor mensal válido');
      return;
    }

    if (!startDate || !endDate) {
      setError('Informe as datas de início e fim');
      return;
    }

    if (new Date(startDate) > new Date(endDate)) {
      setError('A data de início deve ser anterior à data de fim');
      return;
    }

    setLoading(true);

    try {
      const request: RecurringExpenseRequest = {
        userId: user!.id,
        expenseTypeId,
        monthlyAmount: parseFloat(monthlyAmount),
        startDate,
        endDate,
      };

      if (recurringExpense) {
        await recurringExpenseService.updateRecurringExpense(recurringExpense.id, request);
      } else {
        await recurringExpenseService.createRecurringExpense(request);
      }

      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro ao salvar dívida recorrente');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-20">
      <div className="bg-white rounded-xl shadow-2xl p-6 w-full max-w-md mx-4">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-gray-800">
            {recurringExpense ? 'Editar Dívida Recorrente' : 'Nova Dívida Recorrente'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700 text-2xl font-bold"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Tipo de Despesa
            </label>
            <select
              value={expenseTypeId}
              onChange={(e) => setExpenseTypeId(Number(e.target.value))}
              className="w-full px-3 py-2 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              required
            >
              <option value={0}>Selecione...</option>
              {expenseTypes.map((type) => (
                <option key={type.id} value={type.id}>
                  {type.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Valor Mensal (R$)
            </label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              value={monthlyAmount}
              onChange={(e) => setMonthlyAmount(e.target.value)}
              className="w-full px-3 py-2 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="0.00"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Data de Início
            </label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full px-3 py-2 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Data de Fim
            </label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-full px-3 py-2 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              required
            />
          </div>

          {error && (
            <div className="bg-red-50 border-2 border-red-200 text-red-700 px-4 py-2 rounded-lg text-sm">
              {error}
            </div>
          )}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 border-2 border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Salvando...' : recurringExpense ? 'Atualizar' : 'Criar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RecurringExpenseModal;

