#!/usr/local/bin/ruby -w
require 'github/markup'

puts GitHub::Markup.render_s(GitHub::Markups::MARKUP_MARKDOWN, File.read('README.md'))