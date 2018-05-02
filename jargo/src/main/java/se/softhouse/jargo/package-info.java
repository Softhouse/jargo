/*
 * Copyright 2013 Jonatan Jönsson
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * An argument and options parser for java
 * <h2>Glossary</h2>
 * <ul>
 * <li><b>Option</b> a simple flag parameter such as "-l", created with
 * {@link se.softhouse.jargo.Arguments#optionArgument(String, String...)}. Doesn't accept any
 * <i>Parameter</i></li>
 * <li><b>Parameter</b> the second part of an <i>Argument, parsed with a
 * {@link se.softhouse.jargo.StringParser}</i></li>
 * <li><b>{@link se.softhouse.jargo.Argument}</b> both the name and the parameter, such as
 * "--numbers 1,2,3"</li>
 * <li><b>Named Argument</b> an argument with a name such as "--numbers"</li>
 * <li><b>Indexed Argument</b> an argument without a name, such as "file_to_build" in "buildProgram
 * file_to_build"</li>
 * <li><b>{@link se.softhouse.jargo.CommandLineParser}</b> Understands how to manage several
 * <i>Argument</i>s</li>
 * </ul>
 * <h2>Examples</h2>
 * See {@link se.softhouse.jargo.CommandLineParser}, {@link se.softhouse.jargo.Arguments},
 * {@link se.softhouse.jargo.ArgumentBuilder} for how to use this API
 * <h2>Localization</h2>
 * {@link se.softhouse.jargo.Arguments#integerArgument(String...)},
 * {@link se.softhouse.jargo.Arguments#longArgument(String...)} and so on,
 * {@link se.softhouse.common.strings.Describers#numberDescriber() formats} and
 * {@link se.softhouse.common.numbers.NumberType#parse(String, java.util.Locale) parses} numbers in
 * a {@link java.util.Locale} sensitive way.
 * <h2>Inspiration (a.k.a Credits)</h2>
 * If you recognize any features in this API it may come from:
 * <ul>
 * <li><b>git</b> - the {@link se.softhouse.jargo.Command} feature, suggestions for invalid
 * arguments</li>
 * <li><b>maven</b> - executing several commands</li>
 * <li><b>jcommander</b> - almost everything else (this is what I based the feature set on)</li>
 * </ul>
 */
@ParametersAreNonnullByDefault
package se.softhouse.jargo;

import javax.annotation.ParametersAreNonnullByDefault;
