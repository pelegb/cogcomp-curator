#!/usr/bin/perl

#####
# PURPOSE:
#   automate the generation of build.xml files for Curator components.
#

use strict;
use Carp;

croak "Usage: $0 project mainclass testclass libdir"
  unless @ARGV == 4;

my $projectName = shift;
my $mainClassName = shift;
my $testClassName = shift; 
my $libDir = shift;

my $xmlStart = <<'END';
<ivy-module version="2.0">
    <info organisation="edu.illinois.cs.cogcomp" module="illinois-pos-server"/>
    <dependencies>
        <dependency org="commons-lang" name="commons-lang" rev="2.5" transitive="false" conf="* -> *,!sources,!javadoc"/>
        <dependency org="commons-cli" name="commons-cli" rev="1.2" transitive="false" conf="* -> *,!sources,!javadoc"/>
        <dependency org="ch.qos.logback" name="logback-core" rev="0.9.17" transitive="false" conf="* -> *,!sources,!javadoc"/>
        <dependency org="ch.qos.logback" name="logback-classic" rev="0.9.17" transitive="false" conf="* -> *,!sources,!javadoc"/>
        <dependency org="org.slf4j" name="slf4j-api" rev="1.5.8" transitive="false" conf="* -> *,!sources,!javadoc"/>
        <dependency org="edu.illinois.cs.cogcomp" name="LBJ2" rev="" transitive="false" conf="* -> *,!sources,!javadoc"/>
        <dependency org="edu.illinois.cs.cogcomp" name="LBJ2Library" rev="2.8.2" transitive="false" conf="* -> *,!sources,!javadoc"/>
        <dependency org="junit" name="junit" rev="4.4" conf="* -> *,!sources,!javadoc"/>
    </dependencies>
</ivy-module>
END

$xmlStart =~ s/PROJECT/$projectName/;
$xmlStart =~ s/MAINCLASS/$mainClassName/;
$xmlStart =~ s/TESTCLASS/$testClassName/;

my @dependencies = `ls $libDir`; 

foreach my $dep ( @dependencies ) 
{
  chomp $dep;
  $xmlStart.="    <pathelement location=\"\${curator.lib}/$dep\" />\n";
}

my $xmlEnd = <<'LAST';
    <path refid="dependencies.classpath" />
    <pathelement location="${build}" />
  </path>

  <path id="test.classpath">
    <path refid="project.classpath" />
    <pathelement location="${build.test}" />
  </path>

  <target name="compile" description="compile the java files" depends="resolve">
    <mkdir dir="${build}" />
    <javac srcdir="${src}" destdir="${build}" debug="true" classpathref="project.classpath" />
  </target>

  <target name="compile-test" description="compile test cases" depends="compile">
    <mkdir dir="${build.test}" />
    <javac srcdir="${src.test}" destdir="${build.test}" debug="true" classpathref="test.classpath" />
  </target>
  
  <target name="clean" description="removes all java compiled files">
    <delete dir="${build}" />
    <delete dir="${dist}" />
  </target>

  <target name="build" depends="compile" description="alias for compile" />

  <target name="print" description="print command line to run  the server" depends="compile">
    <property name="thecp" refid="project.classpath" />
    <echo>java -Xmx${memory} -classpath ${thecp} ${class.main}</echo>
  </target>

  <target name="dist" description="creates a jar for the server in dist/" depends="clean, compile">
    <mkdir dir="${dist}" />
    <jar destfile="${dist}/${ant.project.name}.jar" basedir="${build}" />
  </target>

  <target name="test" description="run the unit tests" depends="compile-test">
    <junit fork="yes" dir="${basedir}" printsummary="yes" maxmemory="${memory}" failureproperty="tests.failed">
      <test name="${class.test}"/>        
      <formatter type="plain" usefile="true" />
      <classpath refid="test.classpath"/>
    </junit>
    <fail if="tests.failed">Tests failed! Check output!</fail>
  </target>

  <property name="ivy.install.version" value="2.1.0" />
  <condition property="ivy.home" value="${env.IVY_HOME}">
    <isset property="env.IVY_HOME" />
  </condition>

  <property name="ivy.home" value="${user.home}/.ant" />
  <property name="ivy.jar.dir" value="${ivy.home}/lib" />
  <property name="ivy.jar.file" value="${ivy.jar.dir}/ivy.jar" />

  <target name="download-ivy" unless="offline">
    <mkdir dir="${ivy.jar.dir}" />
    <!-- download Ivy from web site so that it can be used even without any special installation -->
    <get src="http://repo2.maven.org/maven2/org/apache/ivy/ivy/${ivy.install.version}/ivy-${ivy.install.version}.jar" dest="${ivy.jar.file}" usetimestamp="true" />
  </target>

  <target name="init-ivy" depends="download-ivy">
    <!-- try to load ivy here from ivy home, in case the user has not already dropped
         it into ant's lib dir (note that the latter copy will always take precedence).
         We will not fail as long as local lib dir exists (it may be empty) and
         ivy is in at least one of ant's lib dir or the local lib dir. -->
    <path id="ivy.lib.path">
      <fileset dir="${ivy.jar.dir}" includes="*.jar" />
    </path>
    <taskdef resource="org/apache/ivy/ant/antlib.xml" uri="antlib:org.apache.ivy.ant" classpathref="ivy.lib.path" />
  </target>

</project>
LAST

print STDOUT "$xmlStart$xmlEnd"; 

exit; 

