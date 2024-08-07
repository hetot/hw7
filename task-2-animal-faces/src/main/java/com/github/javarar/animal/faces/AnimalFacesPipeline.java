package com.github.javarar.animal.faces;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnimalFacesPipeline {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(100);

        Path resources = Paths.get("task-2-animal-faces/src/main/resources/afhq/train/cat");

        double rotationAngle = Math.toRadians(90);

        Set<Future<?>> futures = new HashSet<>();
        for (var imgName : listFilesUsingJavaIO(resources.toAbsolutePath().toString())) {
            var future = executorService.submit(() -> {
                try (var is = Files.newInputStream(resources.toAbsolutePath().resolve(imgName))) {
                    BufferedImage image = ImageIO.read(is);

                    int width = image.getWidth();
                    int height = image.getHeight();
                    int newWidth = (int) Math.abs(width * Math.cos(rotationAngle)) + (int) Math.abs(height * Math.sin(rotationAngle));
                    int newHeight = (int) Math.abs(height * Math.cos(rotationAngle)) + (int) Math.abs(width * Math.sin(rotationAngle));

                    BufferedImage outputImage = new BufferedImage(newWidth, newHeight, image.getType());
                    Graphics2D g2d = outputImage.createGraphics();
                    AffineTransform transform = new AffineTransform();
                    transform.rotate(rotationAngle, (double) newWidth / 2, (double) newHeight / 2);
                    g2d.setTransform(transform);
                    g2d.drawImage(image, 0, 0, null);
                    g2d.dispose();
                    File outputFile = new File(resources.toAbsolutePath().resolve(imgName).toString());
                    ImageIO.write(outputImage, "jpg", outputFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        int done = 0;
        int size = futures.size();
        while (!futures.isEmpty()) {
            Set<Future<?>> toRemove = new HashSet<>();
            for (Future<?> future : futures) {
                try {
                    if (future.isDone()) {
                        future.get();
                        done++;
                        toRemove.add(future);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    toRemove.add(future);
                }
            }
            futures.removeAll(toRemove);
        }
        System.out.println("Done:" + done + " / " + size);
        executorService.shutdown();
    }

    private static Set<String> listFilesUsingJavaIO(String dir) {
        return Stream.of(new File(dir).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
    }
}
