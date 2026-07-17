import { useState, useEffect, useRef } from 'react';
import type { Expense, ExpenseRequest } from '../types';
import ConfirmModal from './ConfirmModal';

interface ExpenseModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (request: ExpenseRequest) => Promise<void>;
  onDelete: (id: number) => Promise<void>;
  expenses: Expense[];
  expenseTypeName: string;
  expenseTypeId: number;
  userId: number;
  month: number;
  year: number;
}

const monthNames = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
];

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);

const ExpenseModal = ({
  isOpen,
  onClose,
  onSave,
  onDelete,
  expenses,
  expenseTypeName,
  expenseTypeId,
  userId,
  month,
  year,
}: ExpenseModalProps) => {
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<Expense | null>(null);
  const amountRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      setAmount('');
      setDescription('');
      setEditingId(null);
      // preventScroll: focar o input não deve rolar a página por baixo do modal.
      setTimeout(() => amountRef.current?.focus({ preventScroll: true }), 0);
    }
  }, [isOpen, month, expenseTypeId]);

  useEffect(() => {
    if (!isOpen) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const total = expenses.reduce((sum, e) => sum + e.amount, 0);

  const startEdit = (expense: Expense) => {
    setEditingId(expense.id);
    setAmount(expense.amount.toString());
    setDescription(expense.description || '');
    amountRef.current?.focus({ preventScroll: true });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setAmount('');
    setDescription('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const numAmount = parseFloat(amount.replace(',', '.'));
    if (isNaN(numAmount) || numAmount <= 0) {
      alert('Informe um valor válido');
      return;
    }

    setSaving(true);
    try {
      await onSave({
        id: editingId ?? undefined,
        userId,
        expenseTypeId,
        amount: numAmount,
        month,
        year,
        description: description.trim() || null,
      });
      onClose();
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    setSaving(true);
    try {
      await onDelete(id);
      if (editingId === id) cancelEdit();
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
    <div
      className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl w-full max-w-md shadow-2xl border border-gray-100 overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-5 py-4 border-b border-slate-100 flex items-start justify-between gap-4">
          <div>
            <h2 className="text-base font-bold text-slate-900">{expenseTypeName}</h2>
            <p className="text-xs text-slate-500">{monthNames[month - 1]} {year}</p>
          </div>
          <div className="text-right">
            <p className="text-xs text-slate-500">Total</p>
            <p className="text-lg font-bold text-slate-900">{formatCurrency(total)}</p>
          </div>
        </div>

        <div className="max-h-56 overflow-y-auto px-5 py-3">
          {expenses.length === 0 ? (
            <p className="text-sm text-slate-400 text-center py-4">Nenhum lançamento nesta célula</p>
          ) : (
            <ul className="space-y-1.5">
              {expenses.map((expense) => (
                <li
                  key={expense.id}
                  className={`flex items-center gap-2 px-3 py-2 rounded-lg border ${
                    editingId === expense.id
                      ? 'border-blue-400 bg-blue-50/60'
                      : expense.userColor === 'blue'
                      ? 'border-blue-200/70 bg-blue-50/40'
                      : 'border-pink-200/70 bg-pink-50/40'
                  }`}
                >
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-slate-800">
                      {formatCurrency(expense.amount)}
                    </p>
                    <p className="text-xs text-slate-500 truncate">
                      {expense.description || <span className="italic text-slate-400">sem descrição</span>}
                      {expense.recurringExpenseId ? ' · recorrente' : ''}
                    </p>
                  </div>
                  <span className="text-[10px] font-medium text-slate-400 shrink-0">
                    {expense.userName}
                  </span>
                  <button
                    type="button"
                    onClick={() => startEdit(expense)}
                    disabled={saving}
                    className="text-xs font-medium text-slate-500 hover:text-blue-600 px-1.5 py-1 rounded disabled:opacity-50"
                  >
                    editar
                  </button>
                  <button
                    type="button"
                    onClick={() => setPendingDelete(expense)}
                    disabled={saving}
                    className="text-xs font-bold text-slate-400 hover:text-red-600 px-1.5 py-1 rounded disabled:opacity-50"
                    title="Excluir lançamento"
                  >
                    ×
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <form onSubmit={handleSubmit} className="px-5 py-4 border-t border-slate-100 bg-slate-50/60">
          <div className="flex gap-2">
            <input
              ref={amountRef}
              type="text"
              inputMode="decimal"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="0,00"
              className="w-28 px-3 py-2 text-sm border-2 border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
            />
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Descrição (opcional)"
              maxLength={255}
              className="flex-1 min-w-0 px-3 py-2 text-sm border-2 border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
            />
          </div>
          <div className="flex items-center justify-between gap-3 mt-3">
            <button
              type="button"
              onClick={editingId ? cancelEdit : onClose}
              className="px-3 py-1.5 text-xs font-medium text-slate-600 hover:text-slate-900"
            >
              {editingId ? 'Cancelar edição' : 'Fechar'}
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 text-xs font-semibold text-white bg-gradient-to-r from-blue-600 to-blue-700 rounded-lg hover:from-blue-700 hover:to-blue-800 shadow disabled:opacity-50"
            >
              {editingId ? 'Salvar alteração' : 'Adicionar lançamento'}
            </button>
          </div>
        </form>
      </div>
    </div>

    {/* Fora do overlay acima: um clique aqui não deve fechar o modal de lançamentos. */}
    <ConfirmModal
        isOpen={pendingDelete !== null}
        title="Excluir lançamento?"
        message={
          pendingDelete
            ? `${formatCurrency(pendingDelete.amount)}${
                pendingDelete.description ? ` · ${pendingDelete.description}` : ''
              }`
            : ''
        }
        onConfirm={() => {
          const target = pendingDelete;
          setPendingDelete(null);
          if (target) void handleDelete(target.id);
        }}
        onCancel={() => setPendingDelete(null)}
      />
    </>
  );
};

export default ExpenseModal;
