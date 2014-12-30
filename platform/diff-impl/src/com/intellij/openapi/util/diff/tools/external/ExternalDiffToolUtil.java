package com.intellij.openapi.util.diff.tools.external;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineTokenizer;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.diff.contents.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.LineSeparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ExternalDiffToolUtil {
  public static boolean canCreateFile(@NotNull DiffContent content) {
    if (content instanceof EmptyContent) return true;
    if (content instanceof DocumentContent) return true;
    if (content instanceof FileContent) return true;
    if (content instanceof DirectoryContent) return ((DirectoryContent)content).getFile().isInLocalFileSystem();
    return false;
  }

  @NotNull
  public static String createFile(@NotNull DiffContent content, @NotNull String title, @NotNull String windowTitle)
    throws IOException {

    if (content instanceof EmptyContent) {
      return createFile(new byte[0], "empty").getPath();
    }
    else if (content instanceof FileContent) {
      VirtualFile file = ((FileContent)content).getFile();
      if (file.isInLocalFileSystem()) {
        return file.getPath();
      }

      byte[] bytes = file.contentsToByteArray();
      return createFile(bytes, getFileName(title, windowTitle, content.getContentType())).getPath();
    }
    else if (content instanceof DocumentContent) {
      final DocumentContent documentContent = (DocumentContent)content;
      FileDocumentManager.getInstance().saveDocument(documentContent.getDocument());

      LineSeparator separator = documentContent.getLineSeparator();
      if (separator == null) separator = LineSeparator.getSystemLineSeparator();

      Charset charset = documentContent.getCharset();
      if (charset == null) charset = Charset.defaultCharset();

      String contentData = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
        @Override
        public String compute() {
          return documentContent.getDocument().getText();
        }
      });
      if (separator != LineSeparator.LF) {
        contentData = StringUtil.convertLineSeparators(contentData, separator.getSeparatorString());
      }

      byte[] bytes = contentData.getBytes(charset);
      return createFile(bytes, getFileName(title, windowTitle, content.getContentType())).getPath();
    }
    else if (content instanceof DirectoryContent) {
      VirtualFile file = ((DirectoryContent)content).getFile();
      if (file.isInLocalFileSystem()) {
        return file.getPath();
      }

      throw new IllegalArgumentException(content.toString());
    }

    throw new IllegalArgumentException(content.toString());
  }

  @NotNull
  public static String getFileName(@NotNull String title, @NotNull String windowTitle, @Nullable FileType fileType) {
    // TODO: keep file name in DiffContent ?
    String ext = fileType != null ? fileType.getDefaultExtension() : ".tmp";
    return title + "." + ext;
  }

  @NotNull
  public static File createFile(@NotNull byte[] bytes, @NotNull String name) throws IOException {
    // TODO Could we make cancelable IO ?
    File tempFile = FileUtil.createTempFile("tmp_", "_" + name, true);
    FileUtil.writeToFile(tempFile, bytes);
    return tempFile;
  }

  public static void execute(@NotNull ExternalDiffSettings settings,
                             @NotNull DiffContent[] contents,
                             @NotNull String[] titles,
                             @NotNull String windowTitle)
    throws IOException, ExecutionException {
    List<String> files = new ArrayList<String>();
    for (int i = 0; i < contents.length; i++) {
      files.add(createFile(contents[i], titles[i], windowTitle));
    }

    CommandLineTokenizer parameterTokenizer = new CommandLineTokenizer(settings.getDiffParameters(), true);

    List<String> args = new ArrayList<String>();
    while (parameterTokenizer.hasMoreTokens()) {
      String arg = parameterTokenizer.nextToken();
      if ("%1".equals(arg)) {
        if (files.size() > 0) args.add(files.get(0));
      }
      else if ("%2".equals(arg)) {
        if (files.size() > 1) args.add(files.get(1));
      }
      else if ("%3".equals(arg)) {
        if (files.size() > 2) args.add(files.get(2));
      }
      else {
        args.add(arg);
      }
    }

    GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(settings.getDiffExePath());

    commandLine.addParameters(args);
    commandLine.createProcess();
  }
}
