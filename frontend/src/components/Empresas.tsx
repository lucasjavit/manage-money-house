import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useConfirm } from '../context/ConfirmContext';
import { companyExpenseService } from '../services/companyExpenseService';
import type { CompanyCategory, CompanyExpense, CompanyCategoryType } from '../types';

const MONTHS = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
];

const formatCurrency = (v: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);

const formatDate = (iso: string) => {
  if (!iso) return '';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
};

const parseAmount = (raw: string): number => parseFloat(raw.replace(/\./g, '').replace(',', '.'));

const Empresas = () => {
  const { user } = useAuth();
  const confirm = useConfirm();
  const now = new Date();

  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [categories, setCategories] = useState<CompanyCategory[]>([]);
  const [items, setItems] = useState<CompanyExpense[]>([]);
  const [loading, setLoading] = useState(true);

  // Form de lançamento
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [dueDate, setDueDate] = useState(now.toISOString().slice(0, 10));

  // Gerenciar categorias
  const [showCatManager, setShowCatManager] = useState(false);
  const [newCatName, setNewCatName] = useState('');
  const [newCatType, setNewCatType] = useState<CompanyCategoryType>('CONTA');

  // Edição inline
  const [editing, setEditing] = useState<number | null>(null);
  const [editDesc, setEditDesc] = useState('');
  const [editAmount, setEditAmount] = useState('');
  const [editCategoryId, setEditCategoryId] = useState<number | ''>('');

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const [cats, list] = await Promise.all([
        companyExpenseService.getCategories(user.id),
        companyExpenseService.getByMonth(user.id, year, month),
      ]);
      setCategories(cats);
      setItems(list);
    } catch (e) {
      console.error('Erro ao carregar empresas:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, year, month]);

  const totalGeral = items.reduce((s, i) => s + i.amount, 0);
  const totalContas = items.filter((i) => i.categoryType === 'CONTA').reduce((s, i) => s + i.amount, 0);
  const totalImpostos = items.filter((i) => i.categoryType === 'IMPOSTO').reduce((s, i) => s + i.amount, 0);

  const addExpense = async () => {
    if (!user) return;
    if (categoryId === '') { alert('Escolha uma categoria'); return; }
    const num = parseAmount(amount);
    if (isNaN(num) || num <= 0) { alert('Informe um valor válido'); return; }
    if (!description.trim()) { alert('Informe uma descrição'); return; }
    try {
      await companyExpenseService.create({
        userId: user.id, categoryId: Number(categoryId),
        description: description.trim(), amount: num, dueDate, month, year,
      });
      setDescription(''); setAmount('');
      await load();
    } catch (e) {
      console.error(e); alert('Erro ao lançar');
    }
  };

  const startEdit = (tx: CompanyExpense) => {
    setEditing(tx.id);
    setEditDesc(tx.description);
    setEditAmount(String(tx.amount).replace('.', ','));
    setEditCategoryId(tx.categoryId);
  };

  const saveEdit = async (id: number) => {
    const num = parseAmount(editAmount);
    if (isNaN(num) || num <= 0) { alert('Valor inválido'); return; }
    try {
      await companyExpenseService.update(id, {
        description: editDesc.trim(), amount: num,
        categoryId: editCategoryId === '' ? undefined : Number(editCategoryId),
      });
      setEditing(null);
      await load();
    } catch (e) { console.error(e); alert('Erro ao salvar'); }
  };

  const removeExpense = async (id: number) => {
    const ok = await confirm({ title: 'Excluir lançamento?', message: 'Será removido do mês.' });
    if (!ok) return;
    try { await companyExpenseService.remove(id); await load(); }
    catch (e) { console.error(e); alert('Erro ao excluir'); }
  };

  const addCategory = async () => {
    if (!user || !newCatName.trim()) { alert('Informe o nome'); return; }
    try {
      await companyExpenseService.createCategory({ userId: user.id, name: newCatName.trim(), type: newCatType });
      setNewCatName('');
      await load();
    } catch (e) { console.error(e); alert('Erro ao criar categoria'); }
  };

  const removeCategory = async (id: number) => {
    const ok = await confirm({ title: 'Excluir categoria?', message: 'Só é possível se não houver lançamentos usando ela.' });
    if (!ok) return;
    try { await companyExpenseService.deleteCategory(id); await load(); }
    catch (e: any) {
      console.error(e);
      alert(e?.response?.data?.error || 'Erro ao excluir categoria');
    }
  };

  const years = Array.from({ length: 6 }, (_, i) => now.getFullYear() - 3 + i);

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h2 className="text-2xl font-bold text-slate-800">Empresas</h2>
        <div className="flex gap-2">
          <select value={month} onChange={(e) => setMonth(Number(e.target.value))}
            className="px-3 py-2 text-sm border-2 border-blue-300/60 rounded-lg bg-white font-medium text-blue-700">
            {MONTHS.map((m, i) => <option key={i} value={i + 1}>{m}</option>)}
          </select>
          <select value={year} onChange={(e) => setYear(Number(e.target.value))}
            className="px-3 py-2 text-sm border-2 border-blue-300/60 rounded-lg bg-white font-medium text-blue-700">
            {years.map((y) => <option key={y} value={y}>{y}</option>)}
          </select>
        </div>
      </div>

      {/* Totais */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="bg-gradient-to-br from-blue-600 to-blue-800 rounded-2xl p-5 text-white">
          <p className="text-xs text-blue-100">Total em {MONTHS[month - 1]} {year}</p>
          <p className="text-2xl font-bold mt-1">{formatCurrency(totalGeral)}</p>
        </div>
        <div className="bg-white/80 rounded-2xl border-2 border-slate-200/60 p-5">
          <p className="text-xs text-slate-500">Contas</p>
          <p className="text-2xl font-bold text-slate-800 mt-1">{formatCurrency(totalContas)}</p>
        </div>
        <div className="bg-white/80 rounded-2xl border-2 border-slate-200/60 p-5">
          <p className="text-xs text-slate-500">Impostos</p>
          <p className="text-2xl font-bold text-slate-800 mt-1">{formatCurrency(totalImpostos)}</p>
        </div>
      </div>

      {/* Novo lançamento */}
      <div className="bg-white/80 rounded-2xl border-2 border-slate-200/60 p-5">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-lg font-semibold text-slate-800">Novo lançamento</h3>
          <button onClick={() => setShowCatManager((v) => !v)}
            className="text-xs font-medium text-blue-600 hover:text-blue-800">
            {showCatManager ? 'Fechar categorias' : 'Gerenciar categorias'}
          </button>
        </div>

        {showCatManager && (
          <div className="mb-4 p-4 rounded-xl bg-slate-50 border border-slate-200">
            <div className="flex flex-wrap gap-2 items-end">
              <input type="text" value={newCatName} onChange={(e) => setNewCatName(e.target.value)}
                placeholder="Nova categoria (ex: Software)"
                className="flex-1 min-w-[140px] px-3 py-2 text-sm border-2 border-slate-300 rounded-lg bg-white" />
              <select value={newCatType} onChange={(e) => setNewCatType(e.target.value as CompanyCategoryType)}
                className="px-3 py-2 text-sm border-2 border-slate-300 rounded-lg bg-white text-slate-800">
                <option value="CONTA">Conta</option>
                <option value="IMPOSTO">Imposto</option>
              </select>
              <button onClick={addCategory}
                className="px-3 py-2 text-sm font-semibold text-white bg-blue-700 rounded-lg hover:bg-blue-800">Adicionar</button>
            </div>
            <div className="mt-3 flex flex-wrap gap-2">
              {categories.map((c) => (
                <span key={c.id} className="inline-flex items-center gap-1 px-2 py-1 text-xs rounded-lg bg-white border border-slate-200 text-slate-700">
                  {c.name} <span className="text-slate-400">({c.type === 'IMPOSTO' ? 'Imposto' : 'Conta'})</span>
                  <button onClick={() => removeCategory(c.id)} className="ml-1 text-slate-300 hover:text-red-600 font-bold">×</button>
                </span>
              ))}
              {categories.length === 0 && <span className="text-xs text-slate-400">Nenhuma categoria ainda.</span>}
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
          <select value={categoryId} onChange={(e) => setCategoryId(e.target.value === '' ? '' : Number(e.target.value))}
            className="px-3 py-2 text-sm border-2 border-slate-300 rounded-lg bg-white text-slate-800">
            <option value="">Categoria…</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <input type="text" value={description} onChange={(e) => setDescription(e.target.value)}
            placeholder="Descrição"
            className="px-3 py-2 text-sm border-2 border-slate-300 rounded-lg bg-white" />
          <input type="text" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)}
            placeholder="Valor (R$)"
            className="px-3 py-2 text-sm border-2 border-slate-300 rounded-lg bg-white" />
          <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)}
            className="px-3 py-2 text-sm border-2 border-slate-300 rounded-lg bg-white" />
        </div>
        <button onClick={addExpense}
          className="mt-3 px-4 py-2 text-sm font-semibold text-white bg-blue-700 rounded-lg hover:bg-blue-800">
          Lançar
        </button>
      </div>

      {/* Lista */}
      {loading ? (
        <div className="text-center text-slate-400 py-8">Carregando...</div>
      ) : items.length === 0 ? (
        <div className="text-center text-slate-400 py-8 bg-white/60 rounded-2xl border-2 border-slate-200/60">
          Nenhum lançamento em {MONTHS[month - 1]} {year}.
        </div>
      ) : (
        <div className="bg-white/80 rounded-2xl border-2 border-slate-200/60 overflow-hidden shadow-sm">
          {items.map((tx, idx) => (
            <div key={tx.id} className={`px-5 py-4 group ${idx > 0 ? 'border-t border-slate-100' : ''}`}>
              {editing === tx.id ? (
                <div className="flex flex-wrap items-center gap-2">
                  <select value={editCategoryId} onChange={(e) => setEditCategoryId(e.target.value === '' ? '' : Number(e.target.value))}
                    className="px-3 py-1.5 text-sm border-2 border-blue-300 rounded-lg bg-white">
                    {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                  <input type="text" value={editDesc} onChange={(e) => setEditDesc(e.target.value)}
                    className="flex-1 min-w-[120px] px-3 py-1.5 text-sm border-2 border-blue-300 rounded-lg bg-white" />
                  <input type="text" inputMode="decimal" value={editAmount} onChange={(e) => setEditAmount(e.target.value)}
                    className="w-24 px-3 py-1.5 text-sm border-2 border-blue-300 rounded-lg bg-white" />
                  <button onClick={() => saveEdit(tx.id)} className="px-3 py-1.5 text-xs font-semibold text-white bg-emerald-600 rounded-lg hover:bg-emerald-700">Salvar</button>
                  <button onClick={() => setEditing(null)} className="px-3 py-1.5 text-xs text-slate-500 hover:text-slate-800">Cancelar</button>
                </div>
              ) : (
                <div className="flex items-center gap-3">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-slate-800 truncate">
                      {tx.description}
                      <span className={`ml-2 text-[10px] font-semibold uppercase px-1.5 py-0.5 rounded ${
                        tx.categoryType === 'IMPOSTO' ? 'bg-amber-100 text-amber-700' : 'bg-blue-100 text-blue-700'
                      }`}>{tx.categoryName}</span>
                    </p>
                    <p className="text-xs text-slate-400">vence {formatDate(tx.dueDate)}</p>
                  </div>
                  <span className="text-sm font-bold text-slate-800 shrink-0">{formatCurrency(tx.amount)}</span>
                  <button onClick={() => startEdit(tx)}
                    className="opacity-0 group-hover:opacity-100 transition-opacity text-xs font-medium text-blue-600 hover:text-blue-800 shrink-0">editar</button>
                  <button onClick={() => removeExpense(tx.id)}
                    className="opacity-0 group-hover:opacity-100 transition-opacity text-slate-300 hover:text-red-600 text-lg font-bold shrink-0" title="Excluir">×</button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Empresas;
