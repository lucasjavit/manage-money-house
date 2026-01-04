import { useState } from 'react';
import type { ExpenseType } from '../types';

interface RecurringExpenseButtonProps {
  expenseTypes: ExpenseType[];
  year: number;
  onCreate: (expenseTypeId: number, startMonth: number, endMonth: number, monthlyAmount: number, startDate?: string, endDate?: string) => Promise<void>;
}

const RecurringExpenseButton = ({ expenseTypes, onCreate }: RecurringExpenseButtonProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [expenseTypeId, setExpenseTypeId] = useState<number>(0);
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [monthlyAmount, setMonthlyAmount] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');

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
      setError('A data de início deve ser anterior ou igual à data de fim');
      return;
    }

    setLoading(true);

    try {
      // Extrair mês de início e fim das datas
      const startMonth = new Date(startDate).getMonth() + 1;
      const endMonth = new Date(endDate).getMonth() + 1;
      
      await onCreate(expenseTypeId, startMonth, endMonth, parseFloat(monthlyAmount), startDate, endDate);
      setIsOpen(false);
      setExpenseTypeId(0);
      setStartDate('');
      setEndDate('');
      setMonthlyAmount('');
      setError('');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro ao criar dívida recorrente');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) {
    return (
      <button
        onClick={() => setIsOpen(true)}
        className="px-4 py-2 text-sm font-semibold rounded-xl transition-all bg-gradient-to-r from-emerald-500 to-emerald-600 text-white hover:from-emerald-600 hover:to-emerald-700 shadow-md hover:shadow-lg ring-2 ring-emerald-400/30"
      >
        Nova Dívida Recorrente
      </button>
    );
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-md mx-4 border-2 border-slate-200/60">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-xl font-bold bg-gradient-to-r from-emerald-600 to-emerald-700 bg-clip-text text-transparent">Nova Dívida Recorrente</h2>
          <button
            onClick={() => {
              setIsOpen(false);
              setError('');
            }}
            className="text-slate-400 hover:text-slate-600 text-2xl font-bold transition-colors w-8 h-8 flex items-center justify-center hover:bg-slate-100 rounded-lg"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1.5">
              Tipo de Despesa
            </label>
            <select
              value={expenseTypeId}
              onChange={(e) => setExpenseTypeId(Number(e.target.value))}
              className="w-full px-3 py-2.5 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-400/50 focus:border-emerald-500 transition-all bg-white shadow-sm hover:border-slate-400"
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

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1.5">
                Data Início
              </label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full px-3 py-2.5 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-400/50 focus:border-emerald-500 transition-all bg-white shadow-sm hover:border-slate-400"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1.5">
                Data Fim
              </label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full px-3 py-2.5 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-400/50 focus:border-emerald-500 transition-all bg-white shadow-sm hover:border-slate-400"
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1.5">
              Valor Mensal (R$)
            </label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              value={monthlyAmount}
              onChange={(e) => setMonthlyAmount(e.target.value)}
              className="w-full px-3 py-2.5 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-400/50 focus:border-emerald-500 transition-all bg-white shadow-sm hover:border-slate-400"
              placeholder="0.00"
              required
            />
          </div>

          {error && (
            <div className="bg-red-50/80 border-2 border-red-200/60 text-red-700 px-4 py-2.5 rounded-lg text-sm font-medium">
              {error}
            </div>
          )}

          <div className="flex gap-3 pt-3">
            <button
              type="button"
              onClick={() => {
                setIsOpen(false);
                setError('');
              }}
              className="flex-1 px-4 py-2.5 border-2 border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 font-semibold transition-all shadow-sm hover:shadow"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2.5 bg-gradient-to-r from-emerald-500 to-emerald-600 text-white rounded-lg hover:from-emerald-600 hover:to-emerald-700 font-semibold transition-all shadow-md hover:shadow-lg ring-2 ring-emerald-400/30 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Salvando...' : 'Criar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RecurringExpenseButton;

