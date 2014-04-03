This is a proof-of-concept utility to bag and transfer content to AP Trust.  The bulk of the code is
written generally enough to be useful by any AP Trust partner institutions, though as an example, the
edu.virginia.lib.aptrust.bags.util.uva package contains the actual scripts UVA used to transfer test
content.

The particularly useful implementations include:

# edu.virginia.lib.aptrust.bags.APTrustBag

This base class implements and enforces most the APTrust-specific bagging requirements.

# Configuration

The code in this package expects local information to be stored in properties files that are
picked up and read by the code and build scripts.

1. bagger.properties (example given)
2. fedora.properties (example given)
3. others, specific to UVA examples.

# UVA Bagging Example

The example code that is used to transfer some sample bags at UVA is included.  To invoke it to run a report:

    mvn clean install exec:java -Dexec.mainClass=edu.virginia.lib.aptrust.bags.util.uva.TrialSubmissionBagger

To invoke it and transfer bags:

    mvn clean install exec:java -Dexec.mainClass=edu.virginia.lib.aptrust.bags.util.uva.TrialSubmissionBagger -Dexec.args="-t"

(add the -o or --overwrite flag to overwrite existing bags)

To invoke it to stage bags (but not transfer them):

    mvn clean install exec:java -Dexec.mainClass=edu.virginia.lib.aptrust.bags.util.uva.TrialSubmissionBagger -Dexec.args="-r"
