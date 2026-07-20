import { createContext, useCallback, useContext, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import ConfirmModal from '../components/ConfirmModal';

interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
}

type ConfirmFn = (options: ConfirmOptions) => Promise<boolean>;

const ConfirmContext = createContext<ConfirmFn | null>(null);

/**
 * Confirmação padrão da aplicação, reaproveitando o ConfirmModal.
 * Uso: const confirm = useConfirm(); if (await confirm({title, message})) { ... }
 */
export const ConfirmProvider = ({ children }: { children: ReactNode }) => {
  const [options, setOptions] = useState<ConfirmOptions | null>(null);
  const resolver = useRef<((value: boolean) => void) | null>(null);

  const confirm = useCallback<ConfirmFn>((opts) => {
    setOptions(opts);
    return new Promise<boolean>((resolve) => {
      resolver.current = resolve;
    });
  }, []);

  const settle = (value: boolean) => {
    resolver.current?.(value);
    resolver.current = null;
    setOptions(null);
  };

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      <ConfirmModal
        isOpen={options !== null}
        title={options?.title || ''}
        message={options?.message || ''}
        confirmLabel={options?.confirmLabel}
        onConfirm={() => settle(true)}
        onCancel={() => settle(false)}
      />
    </ConfirmContext.Provider>
  );
};

export const useConfirm = (): ConfirmFn => {
  const ctx = useContext(ConfirmContext);
  if (!ctx) throw new Error('useConfirm precisa estar dentro de ConfirmProvider');
  return ctx;
};
