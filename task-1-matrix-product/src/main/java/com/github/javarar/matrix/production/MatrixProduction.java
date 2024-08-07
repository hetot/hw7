package com.github.javarar.matrix.production;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.*;

public class MatrixProduction {

    public static Matrix product(Matrix a, Matrix b) {
        int leftColsNum = a.getColsNum();
        int rightRowsNum = b.getRowsNum();
        if (leftColsNum != rightRowsNum) {
            throw new IllegalArgumentException();
        }
        var task = new ComputeResultRunnable(a, b);
        var forkJoinPool = new ForkJoinPool();
        return forkJoinPool.invoke(task);
    }

    @RequiredArgsConstructor
    private static class ComputeResultRunnable extends RecursiveTask<Matrix> {
        private final Matrix a, b;

        @Override
        protected Matrix compute() {
            int rows = a.getRowsNum();
            int cols = b.getColsNum();
            var resultMatrix = new Matrix(rows, cols);
            for (int i = 1; i <= resultMatrix.getRowsNum(); i++) {
                for (int j = 1; j <= resultMatrix.getColsNum(); j++) {
                    var task = new ComputeRowColResult(a, b, i, j);
                    task.fork();
                    resultMatrix.put(i, j, task.join());
                }
            }
            return resultMatrix;
        }

        @RequiredArgsConstructor
        private static class ComputeRowColResult extends RecursiveTask<Integer> {
            private final Matrix a, b;
            private final int i, j;

            @Override
            protected Integer compute() {
                int sum = 0;
                for (int k = 1; k <= i; k++) {
                    sum += (a.get(i, k) * b.get(k, j));
                }
                return sum;
            }
        }
    }

    public static class Matrix {
        private final Map<String, Integer> matrix = new ConcurrentHashMap<>();
        @Getter
        private final int rowsNum;
        @Getter
        private final int colsNum;

        public Matrix(int row, int col) {
            if (row <= 0 || col <= 0) {
                throw new IllegalArgumentException();
            }
            this.rowsNum = row;
            this.colsNum = col;
        }

        public Integer get(int row, int col) {
            validate(row, col);
            return matrix.getOrDefault((row - 1) + "_" + (col - 1), 0);
        }

        public void put(int row, int col, Integer value) {
            validate(row, col);
            matrix.put((row - 1) + "_" + (col - 1), value);
        }

        private void validate(int row, int col) {
            if (row < 0 || row >= rowsNum || col < 0 || col >= colsNum) {
                throw new IllegalArgumentException("row: " + row + ", col: " + col);
            }
        }
    }
}
