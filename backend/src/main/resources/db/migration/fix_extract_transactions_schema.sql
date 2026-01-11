-- Migration script to fix extract_transactions table schema
-- This script removes the old expense_type_id column and ensures extract_expense_type_id is properly configured

-- Step 1: Make expense_type_id nullable (if it still exists and has NOT NULL constraint)
DO $$
BEGIN
    -- Check if column exists and has NOT NULL constraint
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'extract_transactions' 
        AND column_name = 'expense_type_id'
    ) THEN
        -- Make it nullable first
        ALTER TABLE extract_transactions ALTER COLUMN expense_type_id DROP NOT NULL;
        
        -- Drop the column if it's no longer needed
        -- Uncomment the line below if you want to remove it completely
        -- ALTER TABLE extract_transactions DROP COLUMN expense_type_id;
    END IF;
END $$;

-- Step 2: Ensure extract_expense_type_id column exists and is properly configured
DO $$
BEGIN
    -- Check if extract_expense_type_id column exists
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'extract_transactions' 
        AND column_name = 'extract_expense_type_id'
    ) THEN
        -- Create the column if it doesn't exist
        ALTER TABLE extract_transactions 
        ADD COLUMN extract_expense_type_id BIGINT;
        
        -- Add foreign key constraint
        ALTER TABLE extract_transactions 
        ADD CONSTRAINT fk_extract_transaction_expense_type 
        FOREIGN KEY (extract_expense_type_id) 
        REFERENCES extract_expense_types(id);
    END IF;
    
    -- Make sure it's nullable (for now, to allow migration of existing data)
    ALTER TABLE extract_transactions 
    ALTER COLUMN extract_expense_type_id DROP NOT NULL;
END $$;

-- Step 3: Migrate existing data (if any) from expense_type_id to extract_expense_type_id
-- This step is optional and only needed if you have existing data
-- Uncomment if needed:
/*
DO $$
BEGIN
    UPDATE extract_transactions et
    SET extract_expense_type_id = (
        SELECT id 
        FROM extract_expense_types eet 
        WHERE eet.name = 'Outros'
        LIMIT 1
    )
    WHERE et.extract_expense_type_id IS NULL;
END $$;
*/

