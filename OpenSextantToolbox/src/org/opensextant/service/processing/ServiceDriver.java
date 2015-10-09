package org.opensextant.service.processing;

import java.io.File;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

public class ServiceDriver {

  private ServiceDriver() {

  }

  public static void main(String[] args) {

    // the directory of file to send to extractor service
    File inDir = new File(args[0]);

    // host of extractor service
    String extractHost = args[1];

    // how many submitter threads
    int numThread = Integer.parseInt(args[2]);

    // get the collection of files to process
    Collection<File> filesToProcess = FileUtils.listFiles(inDir, FileFilterUtils.trueFileFilter(),
        FileFilterUtils.trueFileFilter());

    // put the files in a thread safe queue
    Queue<File> fileQueue = new ConcurrentLinkedQueue<File>(filesToProcess);

    // the executor that manages the submitter threads
    ExecutorService executor = Executors.newFixedThreadPool(numThread);

    // start the clock
    long start = System.nanoTime();

    for (int i = 0; i < numThread; i++) {
      // create a worker
      ServiceClient worker = new ServiceClient(extractHost);

      // point the worker at the queue
      worker.setFileQueue(fileQueue);

      // add the worker to the pool
      if (!executor.isShutdown()) {
        executor.submit(worker);
      }
    }

    // all files submitted, shutdown (when all finished)
    executor.shutdown();

    // Check if done
    while (!executor.isTerminated()) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        System.err.println();
      }
    }

    // stop the clock and print some stats
    long end = System.nanoTime();
    double dur = (end - start) / 1000000000.0;
    int numDocs = filesToProcess.size();
    double avg = filesToProcess.size() / dur;
    System.out.println(numDocs + " docs took " + dur + " secs. Average= " + avg);

  }

}
