import React, { useState, useRef } from 'react';
import type { B3ReportUploadResponse } from '../../types';
import { realPortfolioService } from '../../services/realPortfolioService';

interface Props {
  userId: number;
  onUploadComplete: (data: B3ReportUploadResponse) => void;
}

const RealPortfolioUpload: React.FC<Props> = ({ userId, onUploadComplete }) => {
  const [isUploading, setIsUploading] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const convertToBase64 = (file: File): Promise<string> => {
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

  const handleFile = async (file: File) => {
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      setError('Por favor, selecione um arquivo PDF.');
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      setError('Arquivo muito grande. Maximo 10MB.');
      return;
    }

    setError(null);
    setIsUploading(true);

    try {
      const base64Content = await convertToBase64(file);

      const response = await realPortfolioService.uploadReport({
        userId,
        fileName: file.name,
        fileContent: base64Content,
      });

      if (response.errorMessage) {
        setError(response.errorMessage);
      } else {
        onUploadComplete(response);
      }
    } catch (err) {
      console.error('Erro ao fazer upload:', err);
      setError('Erro ao processar o arquivo. Tente novamente.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFile(e.dataTransfer.files[0]);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      handleFile(e.target.files[0]);
    }
  };

  const handleClick = () => {
    fileInputRef.current?.click();
  };

  return (
    <div className="max-w-2xl mx-auto">
      <div
        className={`border-2 border-dashed rounded-xl p-12 text-center cursor-pointer transition-all ${
          dragActive
            ? 'border-purple-500 bg-purple-50'
            : 'border-purple-300 hover:border-purple-400 hover:bg-purple-50/50'
        } ${isUploading ? 'pointer-events-none opacity-60' : ''}`}
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        onClick={handleClick}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf"
          onChange={handleChange}
          className="hidden"
        />

        {isUploading ? (
          <div className="space-y-4">
            <div className="animate-spin w-16 h-16 border-4 border-purple-500 border-t-transparent rounded-full mx-auto"></div>
            <p className="text-lg font-medium text-purple-700">
              Processando relatorio...
            </p>
            <p className="text-sm text-gray-500">
              Extraindo dados com IA. Isso pode levar alguns segundos.
            </p>
          </div>
        ) : (
          <>
            <div className="text-6xl mb-4">📄</div>
            <h3 className="text-xl font-bold text-gray-800 mb-2">
              Upload do Relatorio B3
            </h3>
            <p className="text-gray-600 mb-4">
              Arraste e solte seu relatorio consolidado mensal da B3 aqui
            </p>
            <p className="text-sm text-gray-500 mb-6">
              ou clique para selecionar o arquivo
            </p>
            <div className="inline-flex items-center gap-2 px-4 py-2 bg-purple-100 text-purple-700 rounded-lg text-sm">
              <span>📁</span>
              <span>Apenas arquivos PDF (max 10MB)</span>
            </div>
          </>
        )}
      </div>

      {error && (
        <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
          <p className="font-medium">Erro</p>
          <p className="text-sm">{error}</p>
        </div>
      )}

      <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <h4 className="font-medium text-blue-800 mb-2">
          Como obter o relatorio?
        </h4>
        <ol className="text-sm text-blue-700 space-y-1 list-decimal list-inside">
          <li>
            Acesse{' '}
            <a
              href="https://investidor.b3.com.br"
              target="_blank"
              rel="noopener noreferrer"
              className="underline font-medium"
              onClick={(e) => e.stopPropagation()}
            >
              investidor.b3.com.br
            </a>
          </li>
          <li>Faca login com seu CPF e senha</li>
          <li>Va em "Relatorios" → "Consolidado Mensal"</li>
          <li>Selecione o mes e baixe o PDF</li>
        </ol>
      </div>
    </div>
  );
};

export default RealPortfolioUpload;
