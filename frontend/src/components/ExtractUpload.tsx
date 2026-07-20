import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useConfirm } from '../context/ConfirmContext';
import { extractService, type ExtractUploadRequest } from '../services/extractService';
import type { ExpenseInsightsResponse, ExpenseType, ExtractTransaction, IdentifiedTransaction } from '../types';

const ExtractUpload = () => {
  const { user } = useAuth();
  const confirm = useConfirm();
  const navigate = useNavigate();
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [savedTransactions, setSavedTransactions] = useState<ExtractTransaction[]>([]);
  const [loadingTransactions, setLoadingTransactions] = useState(false);
  const [selectedTransactionIds, setSelectedTransactionIds] = useState<Set<number>>(new Set());
  const [expenseTypes, setExpenseTypes] = useState<ExpenseType[]>([]);
  const [selectedExpenseTypeId, setSelectedExpenseTypeId] = useState<number | ''>('');
  const [insights, setInsights] = useState<ExpenseInsightsResponse | null>(null);
  const [loadingInsights, setLoadingInsights] = useState(false);
  // Inicializar sempre com o mês e ano atual
  const getCurrentMonth = () => new Date().getMonth() + 1; // 1-12
  const getCurrentYear = () => new Date().getFullYear();
  
  const [selectedMonth, setSelectedMonth] = useState<number>(getCurrentMonth());
  const [selectedYear, setSelectedYear] = useState<number>(getCurrentYear());
  const [showInsightsModal, setShowInsightsModal] = useState(false);


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

  // Carregar tipos de despesa do banco (tipos específicos para Cartão de Crédito)
  useEffect(() => {
    const loadExpenseTypes = async () => {
      try {
        console.log('Carregando tipos de despesa do Cartão de Crédito...');
        const types = await extractService.getExtractExpenseTypes();
        console.log('Tipos de despesa (Cartão de Crédito) carregados:', types);
        console.log('Quantidade de tipos:', types.length);
        if (types.length === 0) {
          console.warn('Nenhum tipo de despesa encontrado! Verifique se o backend criou os tipos na tabela extract_expense_types.');
        }
        // Ordenar tipos alfabeticamente por nome, mas "Outros" sempre por último
        const sortedTypes = [...types].sort((a, b) => {
          if (a.name === 'Outros') return 1;
          if (b.name === 'Outros') return -1;
          return a.name.localeCompare(b.name, 'pt-BR');
        });
        setExpenseTypes(sortedTypes);
      } catch (err) {
        console.error('Erro ao carregar tipos de despesa:', err);
        alert('Erro ao carregar tipos de despesa. Verifique o console para mais detalhes.');
      }
    };
    loadExpenseTypes();
  }, []);

  // Carregar transações quando user, mês ou ano mudarem
  useEffect(() => {
    if (user && selectedMonth && selectedYear) {
      loadSavedTransactions();
      loadInsights();
    }
  }, [user, selectedMonth, selectedYear]);

  const loadInsights = async () => {
    if (!user) return;
    setLoadingInsights(true);
    try {
      const data = await extractService.getInsights(user.id, selectedMonth, selectedYear);
      setInsights(data);
    } catch (err: any) {
      console.error('Erro ao carregar insights:', err);

      // Mensagem amigável baseada no tipo de erro
      let errorMessage = '❌ Não foi possível carregar a análise financeira.\n\n';

      if (err.code === 'ERR_NETWORK' || err.message?.includes('Network Error')) {
        errorMessage += '🔴 O servidor backend não está respondendo.\n\n';
        errorMessage += '📝 Para iniciar o servidor:\n';
        errorMessage += '1. Abra um terminal na pasta "backend"\n';
        errorMessage += '2. Execute: mvn spring-boot:run\n';
        errorMessage += '3. Aguarde inicializar completamente\n';
        errorMessage += '4. Clique em "Atualizar" novamente';
      } else if (err.response?.status === 404) {
        errorMessage += '🔍 Serviço não encontrado.\n\n';
        errorMessage += 'Verifique se o backend está rodando:\n';
        errorMessage += 'Execute: mvn spring-boot:run na pasta backend';
      } else if (err.response?.status === 500) {
        errorMessage += '⚠️ Erro ao processar análise.\n\n';
        errorMessage += 'Possíveis causas:\n';
        errorMessage += '• Sem transações no período selecionado\n';
        errorMessage += '• Erro no processamento dos dados\n\n';
        errorMessage += 'Tente selecionar outro mês/ano.';
      } else {
        errorMessage += `Detalhes: ${err.message || 'Erro desconhecido'}\n\n`;
        errorMessage += 'Certifique-se de que o backend está rodando.';
      }

      alert(errorMessage);
    } finally {
      setLoadingInsights(false);
    }
  };

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

      // Transformar erro técnico em mensagem amigável
      let errorMessage = 'Erro ao salvar transações.';

      if (err.response?.data?.error) {
        const backendError = err.response.data.error;

        // Detectar erro de constraint do banco (expense_type_id)
        if (backendError.includes('expense_type_id') || backendError.includes('not-null constraint')) {
          errorMessage = 'Algumas transações não puderam ser categorizadas automaticamente. Por favor, revise o extrato e tente novamente.';
        } else if (backendError.includes('duplicate') || backendError.includes('unique')) {
          errorMessage = 'Algumas transações já foram salvas anteriormente.';
        } else {
          errorMessage = 'Erro ao processar as transações. Verifique o arquivo e tente novamente.';
        }
      }

      setError(errorMessage);
      alert(`❌ ${errorMessage}`);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteTransaction = async (id: number) => {
    const ok = await confirm({
      title: 'Excluir transação?',
      message: 'Esta transação do cartão será removida.',
    });
    if (!ok) return;

    try {
      await extractService.deleteTransaction(id);
      setSelectedTransactionIds(new Set());
      await loadSavedTransactions();
    } catch (err) {
      console.error('Erro ao excluir transação:', err);
      alert('Erro ao excluir transação.');
    }
  };

  const handleToggleSelect = (id: number) => {
    const newSelected = new Set(selectedTransactionIds);
    if (newSelected.has(id)) {
      newSelected.delete(id);
    } else {
      newSelected.add(id);
    }
    setSelectedTransactionIds(newSelected);
  };


  const handleDeleteSelected = async () => {
    if (selectedTransactionIds.size === 0) {
      alert('Selecione pelo menos uma transação para excluir.');
      return;
    }
    
    const ok = await confirm({
      title: `Excluir ${selectedTransactionIds.size} transação(ões)?`,
      message: 'As transações selecionadas serão removidas.',
    });
    if (!ok) return;

    try {
      await extractService.deleteTransactions(Array.from(selectedTransactionIds));
      setSelectedTransactionIds(new Set());
      await loadSavedTransactions();
      alert(`${selectedTransactionIds.size} transação(ões) excluída(s) com sucesso!`);
    } catch (err) {
      console.error('Erro ao excluir transações:', err);
      alert('Erro ao excluir transações.');
    }
  };

  const handleUpdateSelectedType = async () => {
    if (selectedTransactionIds.size === 0) {
      alert('Selecione pelo menos uma transação para alterar o tipo.');
      return;
    }
    
    if (!selectedExpenseTypeId) {
      alert('Selecione um tipo de despesa.');
      return;
    }
    
    try {
      await extractService.updateTransactionsType(Array.from(selectedTransactionIds), selectedExpenseTypeId);
      setSelectedTransactionIds(new Set());
      setSelectedExpenseTypeId('');
      await loadSavedTransactions();
      alert(`${selectedTransactionIds.size} transação(ões) atualizada(s) com sucesso!`);
    } catch (err) {
      console.error('Erro ao atualizar tipo das transações:', err);
      alert('Erro ao atualizar tipo das transações.');
    }
  };

  const handleUpdateSingleTransactionType = async (transactionId: number, newExpenseTypeId: number) => {
    try {
      await extractService.updateTransactionType(transactionId, newExpenseTypeId);
      await loadSavedTransactions();
    } catch (err) {
      console.error('Erro ao atualizar tipo da transação:', err);
      alert('Erro ao atualizar tipo da transação.');
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

      {/* Botão Flutuante de Análise Financeira */}
      {savedTransactions.length > 0 && (
        <button
          onClick={() => {
            if (!insights) {
              loadInsights();
            }
            setShowInsightsModal(true);
          }}
          className="fixed bottom-6 right-6 z-40 w-16 h-16 bg-gradient-to-br from-blue-500 to-purple-600 text-white rounded-full shadow-2xl hover:shadow-blue-500/50 hover:scale-110 transition-all flex items-center justify-center group"
          title="Ver Análise Financeira"
        >
          <div className="relative">
            <span className="text-3xl">📊</span>
            {insights && (
              <span className="absolute -top-2 -right-2 bg-green-600 text-white text-xs font-bold rounded-full w-6 h-6 flex items-center justify-center border-2 border-white">
                ✓
              </span>
            )}
          </div>
        </button>
      )}

      {/* Modal de Análise Financeira */}
      {showInsightsModal && (
        <>
          {/* Overlay */}
          <div
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-40 animate-fadeIn"
            onClick={() => setShowInsightsModal(false)}
          />

          {/* Modal */}
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 animate-fadeIn">
            <div className="bg-white rounded-2xl shadow-2xl max-w-6xl w-full max-h-[90vh] overflow-hidden">
              {/* Header */}
              <div className="bg-gradient-to-r from-blue-500 via-purple-600 to-blue-600 text-white px-8 py-6">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    <div className="w-14 h-14 bg-white/20 rounded-xl flex items-center justify-center backdrop-blur-sm">
                      <span className="text-4xl">📊</span>
                    </div>
                    <div>
                      <h2 className="text-2xl font-bold">Análise Financeira Completa</h2>
                      <p className="text-blue-100 text-sm mt-1">
                        Insights detalhados do seu mês
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <button
                      onClick={loadInsights}
                      disabled={loadingInsights}
                      className="px-4 py-2 text-sm font-semibold bg-white/20 hover:bg-white/30 rounded-lg transition-all backdrop-blur-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                    >
                      <span>{loadingInsights ? 'Analisando...' : 'Atualizar'}</span>
                      {loadingInsights ? '⏳' : '🔄'}
                    </button>
                    <button
                      onClick={() => setShowInsightsModal(false)}
                      className="text-white hover:bg-white/20 rounded-lg p-2 transition-colors"
                    >
                      <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                </div>
              </div>

              {/* Conteúdo Scrollable */}
              <div className="overflow-y-auto max-h-[calc(90vh-120px)] p-8 bg-gradient-to-br from-blue-50/30 via-white to-purple-50/30">
                {loadingInsights ? (
                  <div className="flex flex-col items-center justify-center py-12 gap-4">
                    <svg className="animate-spin h-12 w-12 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <p className="text-slate-600 font-medium">Carregando análise financeira...</p>
                  </div>
                ) : insights ? (
                  <div className="space-y-6">
                    {/* Cards Principais - Reorganizados e Otimizados */}
                    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                      {/* 1. Total Gasto - PRINCIPAL */}
                      <div className="bg-gradient-to-br from-red-50 to-red-100 border-2 border-red-200 rounded-xl p-4 shadow-md hover:shadow-lg transition-shadow">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-2xl">💰</span>
                          <span className="text-xs font-semibold text-red-700 bg-red-200 px-2 py-1 rounded-full">Total</span>
                        </div>
                        <p className="text-2xl font-bold text-red-700 mb-1">
                          {new Intl.NumberFormat('pt-BR', {
                            style: 'currency',
                            currency: 'BRL',
                            maximumFractionDigits: 0,
                          }).format(insights.totalSpent || 0)}
                        </p>
                        <p className="text-xs text-red-600">Gastos do mês</p>
                      </div>

                      {/* 2. Tendência vs Mês Anterior - COMPARAÇÃO IMPORTANTE */}
                      {insights.trends && insights.trends.length > 0 && (
                        <div className={`bg-gradient-to-br border-2 rounded-xl p-4 shadow-md hover:shadow-lg transition-shadow ${
                          insights.trends[0].type === 'increase' ? 'from-red-50 to-red-100 border-red-200' :
                          insights.trends[0].type === 'decrease' ? 'from-green-50 to-green-100 border-green-200' :
                          'from-slate-50 to-slate-100 border-slate-200'
                        }`}>
                          <div className="flex items-center justify-between mb-2">
                            <span className="text-2xl">{insights.trends[0].type === 'increase' ? '📈' :
                             insights.trends[0].type === 'decrease' ? '📉' : '➡️'}</span>
                            <span className={`text-xs font-semibold px-2 py-1 rounded-full ${
                              insights.trends[0].type === 'increase' ? 'text-red-700 bg-red-200' :
                              insights.trends[0].type === 'decrease' ? 'text-green-700 bg-green-200' :
                              'text-slate-700 bg-slate-200'
                            }`}>
                              vs Anterior
                            </span>
                          </div>
                          <p className={`text-2xl font-bold mb-1 ${
                            insights.trends[0].type === 'increase' ? 'text-red-700' :
                            insights.trends[0].type === 'decrease' ? 'text-green-700' :
                            'text-slate-700'
                          }`}>
                            {insights.trends[0].value}
                          </p>
                          <p className={`text-xs ${
                            insights.trends[0].type === 'increase' ? 'text-red-600' :
                            insights.trends[0].type === 'decrease' ? 'text-green-600' :
                            'text-slate-600'
                          }`}>
                            Comparação mensal
                          </p>
                        </div>
                      )}

                      {/* 3. Transações - VOLUME */}
                      <div className="bg-gradient-to-br from-blue-50 to-blue-100 border-2 border-blue-200 rounded-xl p-4 shadow-md hover:shadow-lg transition-shadow">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-2xl">📊</span>
                          <span className="text-xs font-semibold text-blue-700 bg-blue-200 px-2 py-1 rounded-full">Total</span>
                        </div>
                        <p className="text-2xl font-bold text-blue-700 mb-1">{insights.totalTransactions || 0}</p>
                        <p className="text-xs text-blue-600">Transações</p>
                      </div>

                      {/* 4. Média por Transação - MÉTRICA */}
                      {insights.averagePerTransaction && (
                        <div className="bg-gradient-to-br from-green-50 to-green-100 border-2 border-green-200 rounded-xl p-4 shadow-md hover:shadow-lg transition-shadow">
                          <div className="flex items-center justify-between mb-2">
                            <span className="text-2xl">📈</span>
                            <span className="text-xs font-semibold text-green-700 bg-green-200 px-2 py-1 rounded-full">Média</span>
                          </div>
                          <p className="text-2xl font-bold text-green-700 mb-1">
                            {new Intl.NumberFormat('pt-BR', {
                              style: 'currency',
                              currency: 'BRL',
                              maximumFractionDigits: 0,
                            }).format(insights.averagePerTransaction)}
                          </p>
                          <p className="text-xs text-green-600">Por transação</p>
                        </div>
                      )}

                      {/* Quick Stats - Filtrados (sem duplicatas) */}
                      {insights.quickStats && insights.quickStats
                        .filter((stat: any) =>
                          // Remover duplicata de "Média/Transação" (já temos averagePerTransaction)
                          !stat.label.includes('Média/Transação') && !stat.label.includes('Média')
                        )
                        .map((stat: any, idx: number) => (
                        <div key={idx} className={`bg-gradient-to-br border-2 rounded-xl p-4 shadow-md hover:shadow-lg transition-shadow ${
                          stat.color === 'red' ? 'from-red-50 to-red-100 border-red-200' :
                          stat.color === 'blue' ? 'from-blue-50 to-blue-100 border-blue-200' :
                          stat.color === 'purple' ? 'from-purple-50 to-purple-100 border-purple-200' :
                          stat.color === 'green' ? 'from-green-50 to-green-100 border-green-200' :
                          stat.color === 'orange' ? 'from-orange-50 to-orange-100 border-orange-200' :
                          'from-slate-50 to-slate-100 border-slate-200'
                        }`}>
                          <div className="flex items-center justify-between mb-2">
                            <span className="text-2xl">{stat.icon}</span>
                            <span className={`text-xs font-semibold px-2 py-1 rounded-full ${
                              stat.color === 'red' ? 'text-red-700 bg-red-200' :
                              stat.color === 'blue' ? 'text-blue-700 bg-blue-200' :
                              stat.color === 'purple' ? 'text-purple-700 bg-purple-200' :
                              stat.color === 'green' ? 'text-green-700 bg-green-200' :
                              stat.color === 'orange' ? 'text-orange-700 bg-orange-200' :
                              'text-slate-700 bg-slate-200'
                            }`}>
                              {stat.label}
                            </span>
                          </div>
                          <p className={`text-2xl font-bold mb-1 ${
                            stat.color === 'red' ? 'text-red-700' :
                            stat.color === 'blue' ? 'text-blue-700' :
                            stat.color === 'purple' ? 'text-purple-700' :
                            stat.color === 'green' ? 'text-green-700' :
                            stat.color === 'orange' ? 'text-orange-700' :
                            'text-slate-700'
                          }`}>
                            {stat.value}
                          </p>
                          <p className={`text-xs ${
                            stat.color === 'red' ? 'text-red-600' :
                            stat.color === 'blue' ? 'text-blue-600' :
                            stat.color === 'purple' ? 'text-purple-600' :
                            stat.color === 'green' ? 'text-green-600' :
                            stat.color === 'orange' ? 'text-orange-600' :
                            'text-slate-600'
                          }`}>
                            {stat.label.includes('Maior') ? 'Gasto único' :
                             stat.label.includes('Top') ? 'Categoria' :
                             stat.label.includes('Transações') ? 'Por dia' :
                             stat.label.includes('Dia') ? 'Dia mais caro' : ''}
                          </p>
                        </div>
                      ))}

                      {/* 5. Dia Mais Caro - DETALHE (se não vier em quickStats) */}
                      {insights.mostExpensiveDay && !insights.quickStats?.some((s: any) => s.label.includes('Dia')) && (
                        <div className="bg-gradient-to-br from-orange-50 to-orange-100 border-2 border-orange-200 rounded-xl p-4 shadow-md hover:shadow-lg transition-shadow">
                          <div className="flex items-center justify-between mb-2">
                            <span className="text-2xl">📅</span>
                            <span className="text-xs font-semibold text-orange-700 bg-orange-200 px-2 py-1 rounded-full">Top</span>
                          </div>
                          <p className="text-xl font-bold text-orange-700 mb-1 truncate">{insights.mostExpensiveDay}</p>
                          <p className="text-xs text-orange-600">Dia mais caro</p>
                        </div>
                      )}
                    </div>

                    {/* Top 3 Gastos e Categorias - Lado a Lado */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                      {/* Top 3 Gastos - Melhorado */}
                      {insights.topExpenses && insights.topExpenses.length > 0 && (
                        <div className="bg-gradient-to-br from-red-50 to-orange-50 rounded-xl p-4 border-2 border-red-200 shadow-sm">
                          <div className="flex items-center gap-2 mb-4">
                            <span className="text-xl">🔥</span>
                            <h3 className="text-base font-bold text-red-800">Maiores Gastos</h3>
                          </div>
                          <div className="space-y-3">
                            {insights.topExpenses.slice(0, 3).map((expense: any, idx: number) => {
                              const medals = ['🥇', '🥈', '🥉'];
                              return (
                                <div key={idx} className="bg-white rounded-lg p-3 shadow-sm border border-red-100 hover:shadow-md transition-shadow">
                                  <div className="flex items-center gap-3">
                                    <span className="text-2xl">{medals[idx]}</span>
                                    <div className="flex-1 min-w-0">
                                      <p className="font-semibold text-slate-900 truncate">{expense.description}</p>
                                      <p className="text-xs text-slate-600">#{idx + 1} maior gasto</p>
                                    </div>
                                    <div className="text-right">
                                      <p className="text-lg font-bold text-red-600">
                                        {new Intl.NumberFormat('pt-BR', {
                                          style: 'currency',
                                          currency: 'BRL',
                                          maximumFractionDigits: 0,
                                        }).format(expense.amount)}
                                      </p>
                                    </div>
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      )}

                      {/* Categorias - Melhoradas */}
                      {insights.categories && insights.categories.length > 0 && (
                        <div className="bg-white rounded-xl p-4 border-2 border-slate-200 shadow-sm">
                          <div className="flex items-center gap-2 mb-4">
                            <span className="text-xl">📁</span>
                            <h3 className="text-base font-bold text-slate-800">Gastos por Categoria</h3>
                          </div>
                          <div className="space-y-3">
                            {insights.categories.slice(0, 5).map((cat: any, idx: number) => {
                              const colors = [
                                'from-blue-500 to-blue-600',
                                'from-purple-500 to-purple-600',
                                'from-pink-500 to-pink-600',
                                'from-orange-500 to-orange-600',
                                'from-green-500 to-green-600'
                              ];
                              return (
                                <div key={idx} className="bg-slate-50 rounded-lg p-3 hover:bg-slate-100 transition-colors">
                                  <div className="flex justify-between items-center mb-2">
                                    <span className="text-sm font-semibold text-slate-800">{cat.categoryName}</span>
                                    <div className="flex items-center gap-2">
                                      <span className="text-xs font-medium text-slate-600">
                                        {new Intl.NumberFormat('pt-BR', {
                                          style: 'currency',
                                          currency: 'BRL',
                                          maximumFractionDigits: 0,
                                        }).format(cat.total)}
                                      </span>
                                      <span className="text-sm font-bold text-slate-900 bg-slate-200 px-2 py-0.5 rounded-full">
                                        {cat.percentage.toFixed(0)}%
                                      </span>
                                    </div>
                                  </div>
                                  <div className="w-full bg-slate-200 rounded-full h-2.5 overflow-hidden mb-2">
                                    <div
                                      className={`bg-gradient-to-r ${colors[idx % colors.length]} h-2.5 rounded-full transition-all duration-500 shadow-sm`}
                                      style={{ width: `${Math.min(cat.percentage, 100)}%` }}
                                    ></div>
                                  </div>
                                  {cat.highestExpense && (
                                    <div className="flex items-center justify-between text-xs pt-2 border-t border-slate-200">
                                      <span className="text-slate-600 flex items-center gap-1">
                                        <span className="text-sm">⬆️</span>
                                        Maior gasto:
                                      </span>
                                      <span className="font-bold text-slate-700">
                                        {new Intl.NumberFormat('pt-BR', {
                                          style: 'currency',
                                          currency: 'BRL',
                                          maximumFractionDigits: 0,
                                        }).format(cat.highestExpense)}
                                      </span>
                                    </div>
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Alertas IA */}
                    {insights.aiInsights?.warnings && insights.aiInsights.warnings.length > 0 && (
                      <div className="bg-gradient-to-br from-yellow-50 to-orange-50 rounded-xl p-4 border-2 border-yellow-300 shadow-sm">
                        <div className="flex items-center gap-2 mb-3">
                          <span className="text-xl">⚠️</span>
                          <h3 className="text-base font-bold text-orange-800">Pontos de Atenção</h3>
                        </div>
                        <div className="space-y-2">
                          {insights.aiInsights.warnings.slice(0, 3).map((warning: string, idx: number) => (
                            <div key={idx} className="bg-white/70 rounded-lg p-3 border border-orange-200">
                              <p className="text-sm text-slate-700 flex items-start gap-2">
                                <span className="text-orange-600 font-bold text-lg leading-none">!</span>
                                <span className="flex-1">{warning}</span>
                              </p>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Padrões de Gastos Detectados - Removido temporariamente até backend implementar */}
                    {/* {insights.aiInsights?.patterns && insights.aiInsights.patterns.length > 0 && (
                      <div className="bg-gradient-to-br from-purple-50 to-indigo-50 rounded-xl p-6 border-2 border-purple-200 shadow-sm">
                        <div className="flex items-center gap-2 mb-4">
                          <span className="text-2xl">🔍</span>
                          <h3 className="text-lg font-bold text-purple-800">Padrões Identificados</h3>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          {insights.aiInsights.patterns.map((pattern: any, idx: number) => (
                            <div
                              key={idx}
                              className="bg-white rounded-lg p-4 border border-purple-200 hover:border-purple-300 transition-colors"
                            >
                              <div className="flex items-start gap-3">
                                <span className="text-2xl">{pattern.icon}</span>
                                <div className="flex-1">
                                  <p className="text-sm font-semibold text-slate-800 mb-1">
                                    {pattern.description}
                                  </p>
                                  <p className="text-xs text-slate-600">{pattern.insight}</p>
                                  <span className={`inline-block mt-2 px-2 py-1 rounded-full text-xs font-semibold ${
                                    pattern.type === 'anomaly' ? 'bg-red-100 text-red-700' :
                                    pattern.type === 'trend' ? 'bg-blue-100 text-blue-700' :
                                    pattern.type === 'temporal' ? 'bg-green-100 text-green-700' :
                                    'bg-purple-100 text-purple-700'
                                  }`}>
                                    {pattern.type}
                                  </span>
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )} */}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-12 gap-4">
                    <span className="text-6xl">📊</span>
                    <p className="text-slate-600 font-medium">Nenhuma análise disponível</p>
                    <button
                      onClick={loadInsights}
                      className="px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-600 text-white font-semibold rounded-lg hover:from-blue-600 hover:to-purple-700 transition-all shadow-md"
                    >
                      Gerar Análise
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}

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
            {/* Barra de ações para seleção múltipla */}
            {selectedTransactionIds.size > 0 && (
              <div className="bg-blue-50 border-2 border-blue-200 rounded-lg p-4 flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <span className="text-sm font-semibold text-blue-700">
                    {selectedTransactionIds.size} transação(ões) selecionada(s)
                  </span>
                  {expenseTypes.length > 0 ? (
                    <select
                      value={selectedExpenseTypeId}
                      onChange={(e) => setSelectedExpenseTypeId(Number(e.target.value))}
                      className="px-3 py-2 text-sm font-medium text-slate-700 bg-white border-2 border-slate-300 rounded-lg hover:bg-slate-50 transition-all focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="">Selecione um tipo</option>
                      {expenseTypes.map((type) => (
                        <option key={type.id} value={type.id}>
                          {type.name}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <span className="text-sm text-slate-500 px-3 py-2">Carregando tipos...</span>
                  )}
                  <button
                    onClick={handleUpdateSelectedType}
                    disabled={!selectedExpenseTypeId}
                    className="px-4 py-2 text-sm font-medium text-white bg-blue-600 border-2 border-blue-700 rounded-lg hover:bg-blue-700 transition-all shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Alterar Tipo
                  </button>
                </div>
                <button
                  onClick={handleDeleteSelected}
                  className="px-4 py-2 text-sm font-medium text-white bg-red-600 border-2 border-red-700 rounded-lg hover:bg-red-700 transition-all shadow-sm"
                >
                  Excluir Selecionadas
                </button>
              </div>
            )}
            
            <div className="text-sm text-slate-600 mb-4">
              Total de transações: {savedTransactions.length}
            </div>
            {groupTransactionsByMonth(savedTransactions).map((group) => {
              const groupTransactionIds = group.transactions.map(t => t.id);
              const allGroupSelected = groupTransactionIds.length > 0 && 
                groupTransactionIds.every(id => selectedTransactionIds.has(id));
              const someGroupSelected = groupTransactionIds.some(id => selectedTransactionIds.has(id));
              
              const handleSelectAllInGroup = () => {
                const newSelected = new Set(selectedTransactionIds);
                if (allGroupSelected) {
                  groupTransactionIds.forEach(id => newSelected.delete(id));
                } else {
                  groupTransactionIds.forEach(id => newSelected.add(id));
                }
                setSelectedTransactionIds(newSelected);
              };
              
              return (
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
                  <table className="min-w-full divide-y divide-slate-100 border-separate border-spacing-0">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-3 py-2 text-center text-xs font-bold text-slate-700 uppercase w-16">#</th>
                        <th className="px-3 py-2 text-left text-xs font-bold text-slate-700 uppercase">
                          <input
                            type="checkbox"
                            checked={allGroupSelected}
                            ref={(input) => {
                              if (input) input.indeterminate = someGroupSelected && !allGroupSelected;
                            }}
                            onChange={handleSelectAllInGroup}
                            className="w-4 h-4 text-blue-600 border-slate-300 rounded focus:ring-blue-500"
                          />
                        </th>
                        <th className="px-3 py-2 text-left text-xs font-bold text-slate-700 uppercase">Data</th>
                        <th className="px-3 py-2 text-left text-xs font-bold text-slate-700 uppercase">Descrição</th>
                        <th className="px-3 py-2 text-left text-xs font-bold text-slate-700 uppercase">Tipo</th>
                        <th className="px-3 py-2 text-right text-xs font-bold text-slate-700 uppercase">Valor</th>
                        <th className="px-3 py-2 text-center text-xs font-bold text-slate-700 uppercase">Ações</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-slate-100">
                      {group.transactions.map((transaction, index) => (
                        <tr 
                          key={transaction.id} 
                          className={`${index % 2 === 0 ? 'bg-white' : 'bg-slate-50'} hover:bg-slate-100 ${selectedTransactionIds.has(transaction.id) ? 'bg-blue-50 hover:bg-blue-100' : ''}`}
                        >
                          <td className="px-3 py-2 text-sm font-semibold text-slate-500 text-center">
                            {index + 1}
                          </td>
                          <td className="px-3 py-2">
                            <input
                              type="checkbox"
                              checked={selectedTransactionIds.has(transaction.id)}
                              onChange={() => handleToggleSelect(transaction.id)}
                              className="w-4 h-4 text-blue-600 border-slate-300 rounded focus:ring-blue-500"
                            />
                          </td>
                          <td className="px-3 py-2 text-sm text-slate-600">
                            {new Date(transaction.transactionDate).toLocaleDateString('pt-BR')}
                          </td>
                          <td className="px-3 py-2 text-sm text-slate-900">{transaction.description}</td>
                          <td className="px-3 py-2">
                            <select
                              value={transaction.expenseTypeId}
                              onChange={(e) => handleUpdateSingleTransactionType(transaction.id, Number(e.target.value))}
                              className="px-2 py-1 text-sm font-medium text-slate-700 bg-white border-2 border-slate-300 rounded-lg hover:bg-slate-50 transition-all focus:outline-none focus:ring-2 focus:ring-blue-500 min-w-[140px]"
                            >
                              {expenseTypes.map((type) => (
                                <option key={type.id} value={type.id}>
                                  {type.name}
                                </option>
                              ))}
                            </select>
                          </td>
                          <td className="px-3 py-2 text-sm font-semibold text-slate-900 text-right">
                            {new Intl.NumberFormat('pt-BR', {
                              style: 'currency',
                              currency: 'BRL',
                            }).format(transaction.amount)}
                          </td>
                          <td className="px-3 py-2 text-center">
                            <button
                              onClick={() => handleDeleteTransaction(transaction.id)}
                              className="text-red-600 hover:text-red-800 p-2 hover:bg-red-50 rounded transition-colors"
                              title="Excluir transação"
                            >
                              <svg
                                xmlns="http://www.w3.org/2000/svg"
                                className="h-5 w-5"
                                fill="none"
                                viewBox="0 0 24 24"
                                stroke="currentColor"
                              >
                                <path
                                  strokeLinecap="round"
                                  strokeLinejoin="round"
                                  strokeWidth={2}
                                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                                />
                              </svg>
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
              );
            })}
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

