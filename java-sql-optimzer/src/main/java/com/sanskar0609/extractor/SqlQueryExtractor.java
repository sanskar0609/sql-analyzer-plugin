package com.sanskar0609.extractor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.sanskar0609.model.ExtractedQuery;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SqlQueryExtractor extends VoidVisitorAdapter<List<ExtractedQuery>> {
    
    private File currentFile;

    public List<ExtractedQuery> extract(List<CompilationUnit> units) {
        List<ExtractedQuery> queries = new ArrayList<>();
        for (CompilationUnit unit : units) {
            unit.getStorage().ifPresent(storage -> {
                this.currentFile = storage.getPath().toFile();
                visit(unit, queries);
            });
        }
        return queries;
    }

    private File getCurrentFile() {
        return currentFile;
    }

    @Override
    public void visit(MethodDeclaration method, List<ExtractedQuery> queries) {
        super.visit(method, queries);

        method.findAll(MethodCallExpr.class).forEach(call -> {
            if (call.getNameAsString().matches("createQuery|nativeQuery|createNativeQuery")) {
                call.getArguments().forEach(argExpr -> {
                    if (argExpr.isStringLiteralExpr()) {
                        String sql = argExpr.asStringLiteralExpr().getValue();
                        int line = method.getBegin().isPresent() ? method.getBegin().get().line : 0;
                        queries.add(new ExtractedQuery(sql, line, getCurrentFile()));
                    }
                });
            }
        });

        method.getAnnotations().forEach(annotation -> {
            if (annotation.getNameAsString().equals("Query")) {

                if (annotation.isSingleMemberAnnotationExpr()) {
                    String sql = annotation.asSingleMemberAnnotationExpr()
                            .getMemberValue().toString().replace("\"", "");
                    int line = method.getBegin().isPresent() ? method.getBegin().get().line : 0;
                    queries.add(new ExtractedQuery(sql, line, getCurrentFile()));
                }

                if (annotation.isNormalAnnotationExpr()) {
                    annotation.asNormalAnnotationExpr().getPairs().forEach(pair -> {
                        if (pair.getNameAsString().equals("value")) {
                            String sql = pair.getValue().toString().replace("\"", "");
                            int line = method.getBegin().isPresent() ? method.getBegin().get().line : 0;
                            queries.add(new ExtractedQuery(sql, line, getCurrentFile()));
                        }
                    });
                }
            }
        });
    }
}
