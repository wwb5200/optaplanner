/*
 * Copyright 2011 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.benchmark.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.commons.io.FilenameUtils;
import org.optaplanner.benchmark.impl.result.PlannerBenchmarkResult;
import org.optaplanner.benchmark.impl.result.ProblemBenchmarkResult;
import org.optaplanner.benchmark.impl.result.SingleBenchmarkResult;
import org.optaplanner.benchmark.impl.result.SolverBenchmarkResult;
import org.optaplanner.benchmark.impl.statistic.ProblemStatistic;
import org.optaplanner.benchmark.impl.statistic.ProblemStatisticType;
import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.core.impl.solution.ProblemIO;
import org.optaplanner.persistence.xstream.XStreamProblemIO;

@XStreamAlias("problemBenchmarks")
public class ProblemBenchmarksConfig {

    private Class<ProblemIO> problemIOClass = null;
    @XStreamImplicit(itemFieldName = "xStreamAnnotatedClass")
    private List<Class> xStreamAnnotatedClassList = null;
    private Boolean writeOutputSolutionEnabled = null;

    @XStreamImplicit(itemFieldName = "inputSolutionFile")
    private List<File> inputSolutionFileList = null;

    @XStreamImplicit(itemFieldName = "problemStatisticType")
    private List<ProblemStatisticType> problemStatisticTypeList = null;

    public Class<ProblemIO> getProblemIOClass() {
        return problemIOClass;
    }

    public void setProblemIOClass(Class<ProblemIO> problemIOClass) {
        this.problemIOClass = problemIOClass;
    }

    public List<Class> getXStreamAnnotatedClassList() {
        return xStreamAnnotatedClassList;
    }

    public void setXStreamAnnotatedClassList(List<Class> xStreamAnnotatedClassList) {
        this.xStreamAnnotatedClassList = xStreamAnnotatedClassList;
    }

    public Boolean getWriteOutputSolutionEnabled() {
        return writeOutputSolutionEnabled;
    }

    public void setWriteOutputSolutionEnabled(Boolean writeOutputSolutionEnabled) {
        this.writeOutputSolutionEnabled = writeOutputSolutionEnabled;
    }

    public List<File> getInputSolutionFileList() {
        return inputSolutionFileList;
    }

    public void setInputSolutionFileList(List<File> inputSolutionFileList) {
        this.inputSolutionFileList = inputSolutionFileList;
    }

    public List<ProblemStatisticType> getProblemStatisticTypeList() {
        return problemStatisticTypeList;
    }

    public void setProblemStatisticTypeList(List<ProblemStatisticType> problemStatisticTypeList) {
        this.problemStatisticTypeList = problemStatisticTypeList;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    public List<ProblemBenchmarkResult> buildProblemBenchmarkList(PlannerBenchmarkResult plannerBenchmark,
            SolverBenchmarkResult solverBenchmarkResult) {
        validate(solverBenchmarkResult);
        ProblemIO problemIO = buildProblemIO();
        List<ProblemBenchmarkResult> problemBenchmarkResultList = new ArrayList<ProblemBenchmarkResult>(inputSolutionFileList.size());
        List<ProblemBenchmarkResult> unifiedProblemBenchmarkResultList = plannerBenchmark.getUnifiedProblemBenchmarkResultList();
        for (File inputSolutionFile : inputSolutionFileList) {
            if (!inputSolutionFile.exists()) {
                throw new IllegalArgumentException("The inputSolutionFile (" + inputSolutionFile + ") does not exist.");
            }
            // 2 SolverBenchmarks containing equal ProblemBenchmarks should contain the same instance
            ProblemBenchmarkResult newProblemBenchmarkResult = buildProblemBenchmark(plannerBenchmark,
                    problemIO, inputSolutionFile);
            ProblemBenchmarkResult problemBenchmarkResult;
            int index = unifiedProblemBenchmarkResultList.indexOf(newProblemBenchmarkResult);
            if (index < 0) {
                problemBenchmarkResult = newProblemBenchmarkResult;
                unifiedProblemBenchmarkResultList.add(problemBenchmarkResult);
            } else {
                problemBenchmarkResult = unifiedProblemBenchmarkResultList.get(index);
            }
            addSingleBenchmark(solverBenchmarkResult, problemBenchmarkResult);
            problemBenchmarkResultList.add(problemBenchmarkResult);
        }
        return problemBenchmarkResultList;
    }

    private void validate(SolverBenchmarkResult solverBenchmarkResult) {
        if (ConfigUtils.isEmptyCollection(inputSolutionFileList)) {
            throw new IllegalArgumentException(
                    "Configure at least 1 <inputSolutionFile> for the solverBenchmarkResult (" + solverBenchmarkResult.getName()
                            + ") directly or indirectly by inheriting it.");
        }
    }

    private ProblemIO buildProblemIO() {
        if (problemIOClass != null && xStreamAnnotatedClassList != null) {
            throw new IllegalArgumentException("Cannot use problemIOClass (" + problemIOClass
                    + ") and xStreamAnnotatedClassList (" + xStreamAnnotatedClassList + ") together.");
        }
        if (problemIOClass != null) {
            return ConfigUtils.newInstance(this, "problemIOClass", problemIOClass);
        } else {
            Class[] xStreamAnnotatedClasses;
            if (xStreamAnnotatedClassList != null) {
                xStreamAnnotatedClasses = xStreamAnnotatedClassList.toArray(new Class[xStreamAnnotatedClassList.size()]);
            } else {
                xStreamAnnotatedClasses = new Class[0];
            }
            return new XStreamProblemIO(xStreamAnnotatedClasses);
        }
    }

    private ProblemBenchmarkResult buildProblemBenchmark(PlannerBenchmarkResult plannerBenchmark,
            ProblemIO problemIO, File inputSolutionFile) {
        ProblemBenchmarkResult problemBenchmarkResult = new ProblemBenchmarkResult(plannerBenchmark);
        String name = FilenameUtils.getBaseName(inputSolutionFile.getName());
        problemBenchmarkResult.setName(name);
        problemBenchmarkResult.setProblemIO(problemIO);
        problemBenchmarkResult.setWriteOutputSolutionEnabled(
                writeOutputSolutionEnabled == null ? false : writeOutputSolutionEnabled);
        problemBenchmarkResult.setInputSolutionFile(inputSolutionFile);
        List<ProblemStatistic> problemStatisticList = new ArrayList<ProblemStatistic>(
                problemStatisticTypeList == null ? 0 : problemStatisticTypeList.size());
        if (problemStatisticTypeList != null) {
            for (ProblemStatisticType problemStatisticType : problemStatisticTypeList) {
                problemStatisticList.add(problemStatisticType.create(problemBenchmarkResult));
            }
        }
        problemBenchmarkResult.setProblemStatisticList(problemStatisticList);
        problemBenchmarkResult.setSingleBenchmarkResultList(new ArrayList<SingleBenchmarkResult>());
        return problemBenchmarkResult;
    }

    private void addSingleBenchmark(
            SolverBenchmarkResult solverBenchmarkResult, ProblemBenchmarkResult problemBenchmarkResult) {
        SingleBenchmarkResult singleBenchmarkResult = new SingleBenchmarkResult(solverBenchmarkResult, problemBenchmarkResult);
        singleBenchmarkResult.initSingleStatisticMap();
        solverBenchmarkResult.getSingleBenchmarkResultList().add(singleBenchmarkResult);
        problemBenchmarkResult.getSingleBenchmarkResultList().add(singleBenchmarkResult);
    }

    public void inherit(ProblemBenchmarksConfig inheritedConfig) {
        problemIOClass = ConfigUtils.inheritOverwritableProperty(problemIOClass,
                inheritedConfig.getProblemIOClass());
        xStreamAnnotatedClassList = ConfigUtils.inheritMergeableListProperty(xStreamAnnotatedClassList,
                inheritedConfig.getXStreamAnnotatedClassList());
        writeOutputSolutionEnabled = ConfigUtils.inheritOverwritableProperty(writeOutputSolutionEnabled,
                inheritedConfig.getWriteOutputSolutionEnabled());
        inputSolutionFileList = ConfigUtils.inheritMergeableListProperty(inputSolutionFileList,
                inheritedConfig.getInputSolutionFileList());
        problemStatisticTypeList = ConfigUtils.inheritMergeableListProperty(problemStatisticTypeList,
                inheritedConfig.getProblemStatisticTypeList());
    }

}