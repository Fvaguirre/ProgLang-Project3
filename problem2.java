import java.io.*;
import java.lang.Thread.*;
import java.util.HashSet;
import java.util.concurrent.*;

class constants {
    public static final int A = 0;
    public static final int Z = 25;
    public static final int numLetters = 26;
}

class TransactionAbortException extends Exception {}
// this is intended to be caught
class TransactionUsageError extends Error {}
// this is intended to be fatal
class InvalidTransactionError extends Error {}
// bad input; will have to skip this transaction

// TO DO: you are not permitted to modify class Account
//
class Account {
    private int value = 0;
    private Thread writer = null;
    private HashSet<Thread> readers;

    public Account(int initialValue) {
        value = initialValue;
        readers = new HashSet<Thread>();
    }

    private void delay() {
        try {
            Thread.sleep(100);  // ms
        } catch(InterruptedException e) {}
            // Java requires you to catch that
    }

    public int peek() {
        delay();
        Thread self = Thread.currentThread();
        synchronized (this) {
            if (writer == self || readers.contains(self)) {
                // should do all peeks before opening account
                // (but *can* peek while another thread has open)
                throw new TransactionUsageError();
            }
            return value;
        }
    }

    // TO DO: the sequential version does not call this method,
    // but the parallel version will need to.
    //
    public void verify(int expectedValue)
        throws TransactionAbortException {
        delay();
        synchronized (this) {
            if (!readers.contains(Thread.currentThread())) {
                throw new TransactionUsageError();
            }
            if (value != expectedValue) {
                // somebody else modified value since we used it;
                // will have to retry
                throw new TransactionAbortException();
            }
        }
    }

    public void update(int newValue) {
        delay();
        synchronized (this) {
            if (writer != Thread.currentThread()) {
                throw new TransactionUsageError();
            }
            value = newValue;
        }
    }

    // TO DO: the sequential version does not open anything for reading
    // (verifying), but the parallel version will need to.
    //
    public void open(boolean forWriting)
        throws TransactionAbortException {
        delay();
        Thread self = Thread.currentThread();
        synchronized (this) {
            if (forWriting) {
                if (writer == self) {
                    throw new TransactionUsageError();
                }
                int numReaders = readers.size();
                if (writer != null || numReaders > 1
                        || (numReaders == 1 && !readers.contains(self))) {
                    // encountered conflict with another transaction;
                    // will have to retry
                    throw new TransactionAbortException();
                }
                writer = self;
            } else {
                if (readers.contains(self) || (writer == self)) {
                    throw new TransactionUsageError();
                }
                if (writer != null) {
                    // encountered conflict with another transaction;
                    // will have to retry
                    throw new TransactionAbortException();
                }
                readers.add(Thread.currentThread());
            }
        }
    }

    public void close() {
        delay();
        Thread self = Thread.currentThread();
        synchronized (this) {
            if (writer != self && !readers.contains(self)) {
                throw new TransactionUsageError();
            }
            if (writer == self) writer = null;
            if (readers.contains(self)) readers.remove(self);
        }
    }

    // print value in wide output field
    public void print() {
        System.out.format("%11d", new Integer(value));
    }

    // print value % numLetters (indirection value) in 2 columns
    public void printMod() {
        int val = value % constants.numLetters;
        if (val < 10) System.out.print("0");
        System.out.print(val);
    }
}

// TO DO: Worker is currently an ordinary class.
// You will need to movify it to make it a task,
// so it can be given to an Executor thread pool.
//
class Worker {
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;

    private Account[] accounts;
    private String transaction;

    // Caches
    private boolean[] isOpen = new boolean[26];
    private boolean[] isRead = new boolean[26];
    private boolean[] isWrite = new boolean[26];
    private int[] read_cache = new int[26];

    // TO DO: The sequential version of Worker peeks at accounts
    // whenever it needs to get a value, and opens, updates, and closes
    // an account whenever it needs to set a value.  This won't work in
    // the parallel version.  Instead, you'll need to cache values
    // you've read and written, and then, after figuring out everything
    // you want to do, (1) open all accounts you need, for reading,
    // writing, or both, (2) verify all previously peeked-at values,
    // (3) perform all updates, and (4) close all opened accounts.

    public Worker(Account[] allAccounts, String trans) {
        accounts = allAccounts;
        transaction = trans;
        for (int i = 0; i < 26; i++) {
            isRead[i] = false;
            isOpen[i] = false;
            isWrite[i] = false;
            read_cache[i] = 0;
        }
    }
    
    // TO DO: parseAccount currently returns a reference to an account.
    // You probably want to change it to return a reference to an
    // account *cache* instead.
    //
    private int parseAccount(String name) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (accounts[accountNum].peek() % numLetters);
        }
        return accountNum;
    }

    private int parseAccountOrNum(String name) {
        int rtn = 0;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            rtn = new Integer(name).intValue();
        } else {
            try {
                rtn = parseAccount(name);
                read_cache[rtn] = accounts[rtn].peek();
                isRead[rtn] = true;
                rtn = read_cache[rtn];
            } catch (TransactionUsageError tue) { 
                System.out.println("Error: peek in parseAccountOrNum failed");
            }
        }
        return rtn;
    }

    public void run() {
        // tokenize transaction
        String[] commands = transaction.split(";");

        for (int i = 0; i < commands.length; i++) {

            String[] words = commands[i].trim().split("\\s");

            if (words.length < 3)
                throw new InvalidTransactionError();

            int transaction_abort = 1;
            int rhs = 0;
            int lhs = 0;
//            while (transaction_abort == 1) {
                transaction_abort = 0;
                lhs = parseAccount(words[0]);
                 System.out.println("LHS value = " + lhs);
                // try to cache the lefthand side
                try {
                    read_cache[lhs] = accounts[lhs].peek();
                    isRead[lhs] = true;
                    isWrite[lhs] = true;
                } catch (TransactionUsageError tue) { 
                    System.out.println("Error: peek on the lhs failed");
                }

                if (!words[1].equals("="))
                    throw new InvalidTransactionError();

                rhs = parseAccountOrNum(words[2]);

                for (int j = 3; j < words.length; j+=2) {
                    if (words[j].equals("+"))
                        rhs += parseAccountOrNum(words[j+1]);
                    else if (words[j].equals("-"))
                        rhs -= parseAccountOrNum(words[j+1]);
                    else
                        throw new InvalidTransactionError();
                }

                // try to open all needed files for reading/writing
                try {
                    for (int foo = 0; foo < 26; foo++) {
                        if (isRead[foo] == true) {
                            if (isWrite[foo] == true) {
                                accounts[foo].open(true);
                            } else {
                                accounts[foo].open(false);
                                accounts[foo].verify(read_cache[foo]);
                            }
                            isOpen[foo] = true;
                        }
                    }
                } catch (TransactionAbortException e) {
                    transaction_abort = 1;
                     // close all open files
                    for (int foo = 0; foo < 26; foo++) {
                        if (isOpen[foo] == true) {
                            accounts[foo].close();
                            isOpen[foo] = false;
                        }
                    }
                    System.out.println("Transaction Aborts");
                    // won't happen in sequential version
                }
//            }

            // try to update the lefthand side
            try {
                accounts[lhs].update(rhs);
            } catch (TransactionUsageError tue) {
                System.out.println("Error: The update has failed.");
            }

            // close all open files
            for (int foo = 0; foo < 26; foo++) {
//                System.out.println("Open Files? " + isOpen[i]);
                if (isOpen[foo] == true) {
                    accounts[foo].close();
                    isOpen[foo] = false;
                    isWrite[foo] = false;
                }
            }
        }

        System.out.println("commit: " + transaction);
    }
}

public class problem2 {
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;
    private static Account[] accounts;
//    private static ExecutorService e = Executors.newFixedThreadPool(26);
    private static ExecutorService e = Executors.newSingleThreadExecutor();

    private static void dumpAccounts() {
        // output values:
        for (int i = A; i <= Z; i++) {
            System.out.print("    ");
            if (i < 10) System.out.print("0");
            System.out.print(i + " ");
            System.out.print(new Character((char) (i + 'A')) + ": ");
            accounts[i].print();
            System.out.print(" (");
            accounts[i].printMod();
            System.out.print(")\n");
        }
    }

    public static void main (String args[])
        throws IOException {
        accounts = new Account[numLetters];
        for (int i = A; i <= Z; i++) {
            accounts[i] = new Account(Z-i);
        }

        // read transactions from input file
        String line;
        BufferedReader input =
            new BufferedReader(new FileReader(args[0]));

// TO DO: you will need to create an Executor and then modify the
// following loop to feed tasks to the executor instead of running them
// directly.  Don't modify the initialization of accounts above, or the
// output at the end.

        System.out.println("initial values:");
        dumpAccounts();

        // Start all tasks
        while ((line = input.readLine()) != null) {
            // need this to pass the line into magic internal class
            final String passLine = line;
            // magic
            Runnable task = new Runnable() {
                public void run() {
                    try {
                        Worker w = new Worker(accounts, passLine);
                        w.run();
                    } catch (InvalidTransactionError ite) {
                        System.err.println("Error: transation '" + passLine + "'is invalid, aborting this transaction");
                    }
                }
            };
            // execute this task
            e.execute(task);
        }

        // nicely request a shutdown of the thread pool
        e.shutdown();

        // wait for all tasks to terminate
        try {
            // Wait a while for existing tasks to terminate
            if (!e.awaitTermination(2,TimeUnit.MINUTES)) {
                e.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!e.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Error: Executor did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            e.shutdownNow();
        }
        // All tasks completed

        System.out.println("final values:");
        dumpAccounts();
    }
}
