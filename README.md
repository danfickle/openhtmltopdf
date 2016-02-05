OPEN HTML TO PDF
---------

OVERVIEW
========
Open HTML to PDF is a pure-Java library for rendering arbitrary well-formed XML 
(or XHTML) using CSS 2.1 for layout and formatting, output to PDF and images.

LICENSE
========
Open HTML to PDF is distributed under the LGPL.  Open HTML to PDF itself is licensed 
under the GNU Lesser General Public License, version 2.1 or later, available at
http://www.gnu.org/copyleft/lesser.html. You can use Open HTML to PDF in any
way and for any purpose you want as long as you respect the terms of the 
license. A copy of the LGPL license is included as license-lgpl-2.1.txt or license-lgpl-3.txt
in our distributions and in our source tree.

Open HTML to PDF uses a couple of FOSS packages to get the job done. A list
of these, along with the license they each have, is listed in the 
LICENSE file in our distribution.   

GETTING FLYING SAUCER
========
New releases of Open HTML to PDF will be distributed through Maven.  Search maven for com.openhtmltopdf.

GETTING STARTED
========
There is a large amount of sample code under the openhtmltopdf-examples directory (integration guide and template guide to come).

CREDITS
========
Open HTML to PDF is based on Flying-saucer. Credit goes to the contributors of that project. Code will also be used from neoFlyingSaucer.

CHANGELOG
========

head
========
+ [Added output device using PDF-BOX 2.0.0 release candidate](https://github.com/danfickle/openhtmltopdf/issues/1)
+ [Make sure XML Document Builder doesn't resolve external DTDs](https://github.com/danfickle/openhtmltopdf/issues/2)
+ [Removed obsolete ITEXT based output devices](https://github.com/danfickle/openhtmltopdf/issues/4)
+ [Removed SWT support](https://github.com/danfickle/openhtmltopdf/issues/6)
+ Regressions (please open issue if required):
	+ PDF form controls.
	+ PDF font types other than built-in and truetype.
	+ XMP PDF metadata in PDFs.
	+ PDF encryption.
	+ [PDF text justification](https://github.com/danfickle/openhtmltopdf/issues/3)
