import { useEffect, useState } from 'react';
import { bankTransactionService } from '../services/bankTransactionService';
import { useConfirm } from '../context/ConfirmContext';
import type { BankTransaction } from '../types';

const MONTHS = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
];

const LUCAS_ID = 1;

const formatCurrency = (v: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);

const formatDate = (iso: string) => {
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
};

const LucasExpenses = () => {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [items, setItems] = useState<BankTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<number | null>(null);
  const [editValue, setEditValue] = useState('');
  const confirm = useConfirm();

  const load = async () => {
    setLoading(true);
    try {
      setItems(await bankTransactionService.list(LUCAS_ID, year, month));
    } catch (e) {
      console.error('Erro ao carregar gastos:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [year, month]);

  const pending = items.filter((i) => i.needsReview);
  const done = items.filter((i) => !i.needsReview);
  const total = done.reduce((s, i) => s + (i.amount ?? 0), 0);

  const saveValue = async (id: number) => {
    const num = parseFloat(editValue.replace(',', '.'));
    if (isNaN(num) || num <= 0) {
      alert('Informe um valor válido');
      return;
    }
    try {
      await bankTransactionService.update(id, { amount: num });
      setEditing(null);
      setEditValue('');
      await load();
    } catch (e) {
      console.error(e);
      alert('Erro ao salvar');
    }
  };

  const remove = async (id: number) => {
    const ok = await confirm({
      title: 'Excluir gasto?',
      message: 'Este gasto será removido dos seus registros.',
    });
    if (!ok) return;
    try {
      await bankTransactionService.remove(id);
      await load();
    } catch (e) {
      console.error(e);
      alert('Erro ao excluir');
    }
  };

  const years = Array.from({ length: 6 }, (_, i) => now.getFullYear() - 3 + i);

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="flex items-center justify-between mb-6 flex-wrap gap-3">
        <h2 className="text-2xl font-bold text-slate-800">Lucas — Gastos</h2>
        <div className="flex gap-2">
          <select
            value={month}
            onChange={(e) => setMonth(Number(e.target.value))}
            className="px-3 py-2 text-sm border-2 border-blue-300/60 rounded-lg bg-white font-medium text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-400/50"
          >
            {MONTHS.map((m, i) => (
              <option key={i} value={i + 1}>{m}</option>
            ))}
          </select>
          <select
            value={year}
            onChange={(e) => setYear(Number(e.target.value))}
            className="px-3 py-2 text-sm border-2 border-blue-300/60 rounded-lg bg-white font-medium text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-400/50"
          >
            {years.map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Total do mês */}
      <div className="bg-gradient-to-br from-blue-500 to-blue-700 rounded-2xl p-6 shadow-lg text-white mb-6">
        <p className="text-sm text-blue-100">Total gasto em {MONTHS[month - 1]} {year}</p>
        <p className="text-4xl font-bold mt-1">{formatCurrency(total)}</p>
        <p className="text-xs text-blue-100 mt-2">
          {done.length} {done.length === 1 ? 'gasto' : 'gastos'}
          {pending.length > 0 && ` · ${pending.length} pendente(s) de valor`}
        </p>
      </div>

      {loading ? (
        <div className="text-center text-slate-400 py-10">Carregando...</div>
      ) : (
        <>
          {/* Pendentes (IA não extraiu o valor) */}
          {pending.length > 0 && (
            <div className="mb-6">
              <h3 className="text-sm font-bold text-amber-700 mb-2">
                Pendentes — informe o valor
              </h3>
              <div className="space-y-2">
                {pending.map((tx) => (
                  <div
                    key={tx.id}
                    className="bg-amber-50 border-2 border-amber-200 rounded-xl p-4"
                  >
                    <p className="text-sm text-slate-700 mb-2">{tx.rawText || tx.description}</p>
                    <div className="flex items-center gap-2">
                      {editing === tx.id ? (
                        <>
                          <input
                            type="text"
                            inputMode="decimal"
                            value={editValue}
                            onChange={(e) => setEditValue(e.target.value)}
                            placeholder="0,00"
                            autoFocus
                            className="w-28 px-3 py-1.5 text-sm border-2 border-amber-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-amber-400/50 bg-white"
                          />
                          <button
                            onClick={() => saveValue(tx.id)}
                            className="px-3 py-1.5 text-xs font-semibold text-white bg-emerald-600 rounded-lg hover:bg-emerald-700"
                          >
                            Salvar
                          </button>
                          <button
                            onClick={() => { setEditing(null); setEditValue(''); }}
                            className="px-3 py-1.5 text-xs text-slate-500 hover:text-slate-800"
                          >
                            Cancelar
                          </button>
                        </>
                      ) : (
                        <>
                          <button
                            onClick={() => { setEditing(tx.id); setEditValue(''); }}
                            className="px-3 py-1.5 text-xs font-semibold text-amber-800 bg-amber-100 rounded-lg hover:bg-amber-200"
                          >
                            Informar valor
                          </button>
                          <button
                            onClick={() => remove(tx.id)}
                            className="px-3 py-1.5 text-xs text-red-600 hover:text-red-800"
                          >
                            Excluir
                          </button>
                        </>
                      )}
                      <span className="ml-auto text-xs text-slate-400">{formatDate(tx.transactionDate)}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Lista de gastos */}
          {done.length === 0 ? (
            <div className="text-center text-slate-400 py-10 bg-white/60 rounded-2xl border-2 border-slate-200/60">
              Nenhum gasto em {MONTHS[month - 1]} {year}.
            </div>
          ) : (
            <div className="bg-white/80 rounded-2xl border-2 border-slate-200/60 overflow-hidden shadow-sm">
              {done.map((tx, idx) => (
                <div
                  key={tx.id}
                  className={`flex items-center gap-3 px-5 py-4 group ${
                    idx > 0 ? 'border-t border-slate-100' : ''
                  }`}
                >
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-slate-800 truncate">{tx.description}</p>
                    <p className="text-xs text-slate-400">{formatDate(tx.transactionDate)}</p>
                  </div>
                  <span className="text-sm font-bold text-slate-800 shrink-0">
                    {formatCurrency(tx.amount ?? 0)}
                  </span>
                  <button
                    onClick={() => remove(tx.id)}
                    className="opacity-0 group-hover:opacity-100 transition-opacity text-slate-300 hover:text-red-600 text-lg font-bold shrink-0"
                    title="Excluir"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default LucasExpenses;
