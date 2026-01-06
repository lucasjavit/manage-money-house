import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { extractService, type ExtractUploadRequest } from '../services/extractService';
import type { IdentifiedTransaction, ExtractTransaction } from '../types';

const ExtractUpload = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [savedTransactions, setSavedTransactions] = useState<ExtractTransaction[]>([]);
  const [loadingTransactions, setLoadingTransactions] = useState(false);
  // Inicializar sempre com o mês e ano atual
  const getCurrentMonth = () => new Date().getMonth() + 1; // 1-12
  const getCurrentYear = () => new Date().getFullYear();
  
  const [selectedMonth, setSelectedMonth] = useState<number>(getCurrentMonth());
  const [selectedYear, setSelectedYear] = useState<number>(getCurrentYear());


  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      const fileType = selectedFile.type;
      if (fileType === 'application/pdf' || fileType.startsWith('image/')) {
        setFile(selectedFile);
        setError(null);
        setLoading(true);
        
        // Processar automaticamente
        try {
          const fileTypeStr = fileType === 'application/pdf' ? 'pdf' : 'png';
          const base64Content = await convertFileToBase64(selectedFile);

          const request: ExtractUploadRequest = {
            fileName: selectedFile.name,
            fileContent: base64Content,
            fileType: fileTypeStr as 'pdf' | 'png',
          };

          // Processar extrato
          const response = await extractService.processExtract(request);
          
          if (response.error) {
            setError(response.error);
            setFile(null);
            return;
          }

          if (response.transactions.length === 0) {
            setError('Nenhuma transação foi identificada no extrato.');
            setFile(null);
            return;
          }

          // Salvar automaticamente todas as transações identificadas
          if (user && response.transactions.length > 0) {
            console.log('Salvando automaticamente', response.transactions.length, 'transações');
            const saveResult = await extractService.saveTransactions({
              userId: user.id,
              transactions: response.transactions,
            });

            if (saveResult.failed === 0) {
              setError(null);
              setFile(null);
              // Recarregar transações do mês atual
              setTimeout(async () => {
                await loadSavedTransactions();
              }, 1000);
              alert(`${saveResult.saved} transação(ões) salva(s) automaticamente!`);
            } else {
              setError(`${saveResult.saved} salva(s), ${saveResult.failed} falha(s). Erros: ${saveResult.errors?.join(', ') || 'Erro desconhecido'}`);
            }
          }
        } catch (err: any) {
          setError(err.response?.data?.error || 'Erro ao processar extrato. Verifique se a API key do OpenAI está configurada.');
          setFile(null);
        } finally {
          setLoading(false);
        }
      } else {
        setError('Por favor, selecione um arquivo PDF ou imagem PNG/JPG');
        setFile(null);
      }
    }
  };

  const convertFileToBase64 = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const base64 = (reader.result as string).split(',')[1];
        resolve(base64);
      };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  };

  const handleReset = () => {
    setFile(null);
    setError(null);
  };

  // Inicializar com mês e ano atual ao montar o componente
  useEffect(() => {
    const now = new Date();
    const currentMonth = now.getMonth() + 1; // 1-12
    const currentYear = now.getFullYear();
    
    // Garantir que os estados estejam com o mês/ano atual
    if (selectedMonth !== currentMonth || selectedYear !== currentYear) {
      setSelectedMonth(currentMonth);
      setSelectedYear(currentYear);
    }
  }, []); // Executar apenas uma vez ao montar

  // Carregar transações quando user, mês ou ano mudarem
  useEffect(() => {
    if (user && selectedMonth && selectedYear) {
      loadSavedTransactions();
    }
  }, [user, selectedMonth, selectedYear]);

  const loadSavedTransactions = async () => {
    if (!user) return;
    setLoadingTransactions(true);
    try {
      // Para cartão de crédito: transações do mês anterior serão pagas no mês selecionado
      // Exemplo: se selecionou Janeiro, buscar transações de Dezembro
      let transactionMonth = selectedMonth - 1;
      let transactionYear = selectedYear;
      
      if (transactionMonth < 1) {
        transactionMonth = 12;
        transactionYear = transactionYear - 1;
      }
      
      console.log('Carregando transações para o usuário:', user.id, 'Mês de pagamento:', selectedMonth, 'Ano:', selectedYear);
      console.log('Buscando transações do mês anterior:', transactionMonth, 'Ano:', transactionYear);
      
      // Calcular último dia do mês anterior (mês da transação)
      const endDate = new Date(transactionYear, transactionMonth, 0); // Último dia do mês
      
      const startDateStr = `${transactionYear}-${String(transactionMonth).padStart(2, '0')}-01`;
      const endDateStr = `${transactionYear}-${String(transactionMonth).padStart(2, '0')}-${String(endDate.getDate()).padStart(2, '0')}`;
      
      console.log('Filtrando por:', { startDateStr, endDateStr, transactionMonth, transactionYear });
      
      const transactions = await extractService.getTransactionsByDateRange(user.id, startDateStr, endDateStr);
      console.log('Transações carregadas (filtradas):', transactions.length, transactions);
      setSavedTransactions(transactions);
    } catch (err) {
      console.error('Erro ao carregar transações salvas:', err);
    } finally {
      setLoadingTransactions(false);
    }
  };

  // Função para salvar transações (não utilizada atualmente)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const handleSaveTransactions = async (transactionsToSave: IdentifiedTransaction[]) => {
    if (!user) return;

    setLoading(true);
    setError(null);

    try {
      console.log('Salvando transações:', transactionsToSave);
      const saveResult = await extractService.saveTransactions({
        userId: user.id,
        transactions: transactionsToSave,
      });

      console.log('Resultado do salvamento:', saveResult);

      if (saveResult.failed === 0) {
        setError(null);
        setFile(null);
        // Aguardar um pouco antes de recarregar para garantir que o banco foi atualizado
        console.log('Aguardando 1 segundo antes de recarregar transações...');
        setTimeout(async () => {
          console.log('Recarregando transações após salvamento...');
          await loadSavedTransactions();
        }, 1000);
        alert(`${saveResult.saved} transação(ões) salva(s) com sucesso!`);
      } else {
        setError(`${saveResult.saved} salva(s), ${saveResult.failed} falha(s). Erros: ${saveResult.errors?.join(', ') || 'Erro desconhecido'}`);
        // Mesmo com falhas, recarregar as que foram salvas
        setTimeout(async () => {
          await loadSavedTransactions();
        }, 500);
      }
    } catch (err: any) {
      console.error('Erro ao salvar transações:', err);
      const errorMessage = err.response?.data?.error || err.message || 'Erro ao salvar transações.';
      setError(errorMessage);
      alert(`Erro: ${errorMessage}`);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteTransaction = async (id: number) => {
    if (!confirm('Deseja realmente excluir esta transação?')) return;
    
    try {
      await extractService.deleteTransaction(id);
      await loadSavedTransactions();
    } catch (err) {
      console.error('Erro ao excluir transação:', err);
      alert('Erro ao excluir transação.');
    }
  };

  // Agrupar transações por mês/ano de pagamento (mês seguinte à transação)
  const groupTransactionsByMonth = (transactions: ExtractTransaction[]) => {
    const grouped: { [key: string]: ExtractTransaction[] } = {};
    
    // Para cartão de crédito: transações são agrupadas pelo mês de pagamento (mês seguinte)
    // Exemplo: transação de 11/12/2025 será agrupada em Janeiro (mês de pagamento)
    transactions.forEach((transaction) => {
      // Parsear a data da transação
      const dateParts = transaction.transactionDate.split('-');
      const transactionYear = parseInt(dateParts[0], 10);
      const transactionMonth = parseInt(dateParts[1], 10);
      
      // Calcular mês de pagamento (mês seguinte)
      let paymentMonth = transactionMonth + 1;
      let paymentYear = transactionYear;
      
      if (paymentMonth > 12) {
        paymentMonth = 1;
        paymentYear = paymentYear + 1;
      }
      
      // Filtrar apenas transações que serão pagas no mês/ano selecionado
      if (paymentYear === selectedYear && paymentMonth === selectedMonth) {
        // Criar chave no formato YYYY-MM (mês de pagamento)
        const monthYear = `${paymentYear}-${String(paymentMonth).padStart(2, '0')}`;
        
        if (!grouped[monthYear]) {
          grouped[monthYear] = [];
        }
        grouped[monthYear].push(transaction);
      }
    });
    
    console.log('Transações agrupadas por mês de pagamento:', Object.keys(grouped).length, 'grupos');

    // Nomes dos meses em português
    const monthNames = [
      'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
      'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'
    ];

    // Converter para array e ordenar por mês/ano (mais recente primeiro)
    return Object.entries(grouped)
      .map(([key, transactions]) => {
        const [year, month] = key.split('-');
        const monthIndex = parseInt(month, 10) - 1; // Converter para índice (0-11)
        const monthYearLabel = `${monthNames[monthIndex]} de ${year}`;
        
        return {
          monthYear: key,
          monthYearLabel: monthYearLabel,
          transactions: transactions.sort((a, b) => {
            // Ordenar por data (mais recente primeiro)
            const dateA = a.transactionDate;
            const dateB = b.transactionDate;
            return dateB.localeCompare(dateA);
          }),
          total: transactions.reduce((sum, t) => sum + t.amount, 0),
        };
      })
      .sort((a, b) => b.monthYear.localeCompare(a.monthYear));
  };


  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      {/* Seção de Upload */}
      <div className="bg-white/80 backdrop-blur-sm rounded-2xl shadow-lg border-2 border-slate-200/60 p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold text-slate-800">Upload de Cartão de Crédito</h2>
          <button
            onClick={() => navigate('/')}
            className="px-4 py-2 text-sm font-medium text-slate-700 bg-slate-100 border-2 border-slate-300 rounded-xl hover:bg-slate-200 transition-all shadow-sm hover:shadow flex items-center gap-2"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-4 w-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M10 19l-7-7m0 0l7-7m-7 7h18"
              />
            </svg>
            Voltar para Dashboard
          </button>
        </div>

        <div className="space-y-6">
          {/* Upload de Arquivo */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Arquivo do Cartão de Crédito (PDF ou PNG)
            </label>
            <div className={`border-2 border-dashed rounded-lg p-6 text-center transition-colors ${
              file 
                ? 'border-blue-400 bg-blue-50/50' 
                : 'border-slate-300 hover:border-blue-400'
            }`}>
              <input
                type="file"
                accept=".pdf,.png,.jpg,.jpeg"
                onChange={handleFileChange}
                className="hidden"
                id="file-upload"
              />
              <label
                htmlFor="file-upload"
                className="cursor-pointer flex flex-col items-center gap-2"
              >
                {file ? (
                  <>
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      className="h-12 w-12 text-blue-500"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                      />
                    </svg>
                    <div className="mt-2">
                      <span className="text-blue-700 font-semibold block">
                        {file.name}
                      </span>
                      <span className="text-xs text-blue-600 mt-1 block">
                        {(file.size / 1024 / 1024).toFixed(2)} MB
                      </span>
                    </div>
                  </>
                ) : (
                  <>
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      className="h-12 w-12 text-slate-400"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
                      />
                    </svg>
                    <span className="text-slate-600 font-medium">
                      Clique para selecionar arquivo
                    </span>
                    <span className="text-xs text-slate-500">
                      PDF ou PNG até 10MB
                    </span>
                  </>
                )}
              </label>
            </div>
            {file && (
              <div className="flex items-center justify-between mt-2 p-3 bg-blue-50 rounded-lg border border-blue-200">
                <div className="flex items-center gap-2">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-5 w-5 text-blue-600"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                    />
                  </svg>
                  <span className="text-sm text-blue-700 font-medium">
                    Arquivo selecionado: {file.name}
                  </span>
                </div>
                <button
                  onClick={() => setFile(null)}
                  className="text-sm text-red-600 hover:text-red-700 font-semibold px-2 py-1 hover:bg-red-50 rounded transition-colors"
                >
                  Remover
                </button>
              </div>
            )}
          </div>

          {error && (
            <div className="bg-red-50 border-2 border-red-200 text-red-700 px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          {loading && (
            <div className="bg-blue-50 border-2 border-blue-200 text-blue-700 px-4 py-3 rounded-lg flex items-center gap-3">
              <svg className="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>Processando e salvando transações automaticamente...</span>
            </div>
          )}

          {file && !loading && (
            <div className="flex gap-3">
              <button
                onClick={handleReset}
                className="px-4 py-3 border-2 border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors font-semibold"
              >
                Limpar
              </button>
            </div>
          )}

        </div>
      </div>

      {/* Seção de Transações Salvas */}
      <div className="bg-white/80 backdrop-blur-sm rounded-2xl shadow-lg border-2 border-slate-200/60 p-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-2xl font-bold text-slate-800">Transações do Cartão de Crédito</h2>
            <p className="text-sm text-slate-600 mt-1">
              Selecione o mês de <strong>pagamento</strong> da fatura. As transações exibidas são do mês anterior.
            </p>
          </div>
          <div className="flex items-center gap-3">
            {/* Seletor de Mês */}
            <select
              value={selectedMonth}
              onChange={(e) => setSelectedMonth(parseInt(e.target.value, 10))}
              className="px-3 py-2 text-sm font-medium text-slate-700 bg-white border-2 border-slate-300 rounded-lg hover:bg-slate-50 transition-all focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value={1}>Janeiro</option>
              <option value={2}>Fevereiro</option>
              <option value={3}>Março</option>
              <option value={4}>Abril</option>
              <option value={5}>Maio</option>
              <option value={6}>Junho</option>
              <option value={7}>Julho</option>
              <option value={8}>Agosto</option>
              <option value={9}>Setembro</option>
              <option value={10}>Outubro</option>
              <option value={11}>Novembro</option>
              <option value={12}>Dezembro</option>
            </select>
            
            {/* Seletor de Ano */}
            <select
              value={selectedYear}
              onChange={(e) => setSelectedYear(parseInt(e.target.value, 10))}
              className="px-3 py-2 text-sm font-medium text-slate-700 bg-white border-2 border-slate-300 rounded-lg hover:bg-slate-50 transition-all focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {Array.from({ length: 10 }, (_, i) => {
                const year = new Date().getFullYear() - 2 + i;
                return (
                  <option key={year} value={year}>
                    {year}
                  </option>
                );
              })}
            </select>
            
            <button
              onClick={loadSavedTransactions}
              disabled={loadingTransactions}
              className="px-4 py-2 text-sm font-medium text-blue-700 bg-blue-100 border-2 border-blue-300 rounded-lg hover:bg-blue-200 transition-all shadow-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className={`h-4 w-4 ${loadingTransactions ? 'animate-spin' : ''}`}
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                />
              </svg>
              {loadingTransactions ? 'Carregando...' : 'Recarregar'}
            </button>
          </div>
        </div>

        {loadingTransactions ? (
          <div className="flex justify-center items-center py-8">
            <svg className="animate-spin h-8 w-8 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          </div>
        ) : savedTransactions.length > 0 ? (
          <div className="space-y-6">
            <div className="text-sm text-slate-600 mb-4">
              Total de transações: {savedTransactions.length}
            </div>
            {groupTransactionsByMonth(savedTransactions).map((group) => (
              <div key={group.monthYear} className="border-2 border-slate-200 rounded-xl overflow-hidden">
                {/* Cabeçalho do Mês */}
                <div className="bg-gradient-to-r from-blue-50 to-blue-100 border-b-2 border-blue-200 px-6 py-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-lg font-bold text-slate-800 capitalize">
                        {group.monthYearLabel} (Mês de Pagamento)
                      </h3>
                      <p className="text-sm text-slate-600 mt-1">
                        {group.transactions.length} {group.transactions.length === 1 ? 'transação' : 'transações'} do mês anterior
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm text-slate-600">Total da fatura</p>
                      <p className="text-xl font-bold text-blue-700">
                        {new Intl.NumberFormat('pt-BR', {
                          style: 'currency',
                          currency: 'BRL',
                        }).format(group.total)}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Tabela de Transações do Mês */}
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-slate-200">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-bold text-slate-700 uppercase">Data</th>
                        <th className="px-4 py-3 text-left text-xs font-bold text-slate-700 uppercase">Descrição</th>
                        <th className="px-4 py-3 text-left text-xs font-bold text-slate-700 uppercase">Tipo</th>
                        <th className="px-4 py-3 text-right text-xs font-bold text-slate-700 uppercase">Valor</th>
                        <th className="px-4 py-3 text-center text-xs font-bold text-slate-700 uppercase">Ações</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-slate-200">
                      {group.transactions.map((transaction) => (
                        <tr key={transaction.id} className="hover:bg-slate-50">
                          <td className="px-4 py-3 text-sm text-slate-600">
                            {new Date(transaction.transactionDate).toLocaleDateString('pt-BR')}
                          </td>
                          <td className="px-4 py-3 text-sm text-slate-900">{transaction.description}</td>
                          <td className="px-4 py-3 text-sm text-slate-700">{transaction.expenseTypeName}</td>
                          <td className="px-4 py-3 text-sm font-semibold text-slate-900 text-right">
                            {new Intl.NumberFormat('pt-BR', {
                              style: 'currency',
                              currency: 'BRL',
                            }).format(transaction.amount)}
                          </td>
                          <td className="px-4 py-3 text-center">
                            <button
                              onClick={() => handleDeleteTransaction(transaction.id)}
                              className="text-red-600 hover:text-red-800 font-semibold px-2 py-1 hover:bg-red-50 rounded transition-colors"
                            >
                              Excluir
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-16 w-16 text-slate-400 mx-auto mb-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
              />
            </svg>
            <p className="text-slate-600 font-medium">Nenhuma transação salva ainda</p>
                <p className="text-sm text-slate-500 mt-2">Faça upload de um cartão de crédito para começar</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default ExtractUpload;

