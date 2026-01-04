import { useState, useEffect } from 'react';
import type { ExpenseType, ExpenseRequest } from '../types';

interface ExpenseModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (request: ExpenseRequest) => void;
  expenseTypes: ExpenseType[];
  userId: number;
  month: number;
  year: number;
  currentValue?: number;
}

const ExpenseModal = ({
  isOpen,
  onClose,
  onSave,
  expenseTypes,
  userId,
  month,
  year,
  currentValue,
}: ExpenseModalProps) => {
  const [selectedTypeId, setSelectedTypeId] = useState<number>(expenseTypes[0]?.id || 0);
  const [amount, setAmount] = useState<string>(currentValue?.toString() || '');

  useEffect(() => {
    if (isOpen) {
      setAmount(currentValue?.toString() || '');
    }
  }, [isOpen, currentValue]);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount < 0) {
      alert('Por favor, insira um valor válido');
      return;
    }

    onSave({
      userId,
      expenseTypeId: selectedTypeId,
      amount: numAmount,
      month,
      year,
    });
    onClose();
  };

  const monthNames = [
    'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
  ];

  return (
    <div className="fixed inset-0 bg-black bg-opacity-20 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl p-8 w-full max-w-md shadow-2xl border border-gray-100 animate-in fade-in zoom-in duration-200 backdrop-blur-none">
        <div className="mb-6">
          <h2 className="text-2xl font-bold text-gray-900 mb-1">
            Adicionar Despesa
          </h2>
          <p className="text-sm text-gray-500">
            {monthNames[month - 1]} {year}
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              Tipo de Despesa
            </label>
            <select
              value={selectedTypeId}
              onChange={(e) => setSelectedTypeId(Number(e.target.value))}
              className="w-full px-4 py-3 border-2 border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all bg-white shadow-sm hover:border-gray-300"
              required
            >
              {expenseTypes.map((type) => (
                <option key={type.id} value={type.id}>
                  {type.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              Valor (R$)
            </label>
            <input
              type="number"
              step="0.01"
              min="0"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="w-full px-4 py-3 border-2 border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all shadow-sm hover:border-gray-300"
              placeholder="0.00"
              required
            />
          </div>

          <div className="flex gap-3 justify-end pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-6 py-2.5 text-gray-700 bg-gray-100 rounded-xl hover:bg-gray-200 font-medium transition-all border border-gray-200"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-6 py-2.5 text-white bg-gradient-to-r from-blue-600 to-blue-700 rounded-xl hover:from-blue-700 hover:to-blue-800 font-semibold shadow-lg hover:shadow-xl transition-all"
            >
              Salvar
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ExpenseModal;

