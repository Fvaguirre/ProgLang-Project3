/*
 * Programming Languages Project 3
 * Problem2.java
 * Eric Yanulis & Tim McMullan
 * 2012-12-07
 */

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


    public Worker(Account[] allAccounts, String trans) {
        accounts = allAccounts;
        transaction = trans;
        for (int i = 0; i < 26; i++) {  //normalize state of all accounts
            isRead[i] = false;
            isOpen[i] = false;
            isWrite[i] = false;
            read_cache[i] = 0;
        }
    }
    
    // parseAccount: returns numeric value account number
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
    

    //parseAccountOrNum: returns number if number, or sets read flag
    //and returns cached account value
    private int parseAccountOrNum(String name) {
        int rtn = 0;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            rtn = new Integer(name).intValue();
        } else {
            rtn = parseAccount(name);
            try {
                read_cache[rtn] = accounts[rtn].peek();
                isRead[rtn] = true;
                rtn = read_cache[rtn];
            } catch (TransactionUsageError tue) { 
                System.err.println("Error: peek in parseAccountOrNum failed");
            }
        }
        return rtn;
    }


    //wooooo runnable
    public void run() {
        // tokenize transaction
        String[] commands = transaction.split(";");

        for (int i = 0; i < commands.length; i++) {
            //Milanova magic...?
            String[] words = commands[i].trim().split("\\s");

            //complain if the transaction syntax is wrong
            if (words.length < 3)
                throw new InvalidTransactionError();

            int transaction_abort = 1;          //transaction abort flag
            int rhs = 0;                        //initialize to safe value
            int lhs = parseAccount(words[0]);   //lhs is the acct to be modified
            isWrite[lhs] = true;                //...so we flag it as writable

            while (transaction_abort == 1) {
                transaction_abort = 0;
                // try to cache the lefthand side by peeking & flagging as readable
                try {
                    read_cache[lhs] = accounts[lhs].peek();
                    isRead[lhs] = true;
                } catch (TransactionUsageError tue) { 
                    System.err.println("Error: peek on the lhs failed");
                }

                if (!words[1].equals("="))      //more checking for invalid transaction syntax
                    throw new InvalidTransactionError();

                rhs = parseAccountOrNum(words[2]);      //grab the first account after the =

                for (int j = 3; j < words.length; j+=2) {   //set rhs depending on the operation
                    if (words[j].equals("+"))
                        rhs += parseAccountOrNum(words[j+1]);
                    else if (words[j].equals("-"))
                        rhs -= parseAccountOrNum(words[j+1]);
                    else
                        throw new InvalidTransactionError();
                }

                // try to open all needed files for reading/writing
                try {
                    for (int foo = 0; foo < 26; foo++) {    //check for read flags & open needed accts
                        if (isRead[foo] == true) {
                            if (isWrite[foo] == true) {
                                accounts[foo].open(true);
                                isOpen[foo] = true;
                            } else {
                                accounts[foo].open(false);
                                isOpen[foo] = true;
                                accounts[foo].verify(read_cache[foo]);
                            }
                        }
                    }
                } catch (TransactionAbortException e) { //if we mess up, close all accounts
                    transaction_abort = 1;
                     // close all open files
                    for (int foo = 0; foo < 26; foo++) {
                        if (isOpen[foo] == true) {
                            accounts[foo].close();
                            isOpen[foo] = false;
                        }
                    }
                }
            }

            // try to update the lefthand side - write our calculated value to LHS
            try {
                accounts[lhs].update(rhs);
            } catch (TransactionUsageError tue) {
                System.err.println("Error: The update has failed.");
            }

            // close all open files
            for (int foo = 0; foo < 26; foo++) {
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
    private static ExecutorService e = Executors.newFixedThreadPool(26); // Declared number of threads

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


        // Start all tasks
        while ((line = input.readLine()) != null) {
            // need this to pass the line into magic internal class
            final String passLine = line;
            // "magic" aka make a wrapper Runnable that calls worker.run()
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
                // Cancel currently executing tasks if they last way too long
                e.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!e.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Error: Executor did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            e.shutdownNow();
        }
        // All tasks completed or killed


        System.out.println("final values:");
        dumpAccounts();
    }
}
